package org.etieskrill.engine.graphics.model.loader;

import org.etieskrill.engine.entity.data.AABB;
import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.graphics.animation.Animation;
import org.etieskrill.engine.graphics.model.Bone;
import org.etieskrill.engine.graphics.model.Mesh;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.Node;
import org.etieskrill.engine.graphics.util.AssimpUtils;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AIScene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.etieskrill.engine.graphics.model.loader.AnimationLoader.loadAnimations;
import static org.etieskrill.engine.graphics.model.loader.Importer.importScene;
import static org.etieskrill.engine.graphics.model.loader.MaterialLoader.loadEmbeddedTextures;
import static org.etieskrill.engine.graphics.model.loader.MaterialLoader.loadMaterials;
import static org.etieskrill.engine.graphics.model.loader.MeshProcessor.loadMeshes;
import static org.lwjgl.assimp.Assimp.*;

public class Loader {

    private static final Logger logger = LoggerFactory.getLogger(Loader.class);

    //TODO loading animations is probs to be separated from model loading: either
    // - load bones with animation every time, and resolve stuff when assigning an animation to a model/animator or
    // - require model/nodes when loading in order to link dependencies directly on load
    public static List<Animation> loadModelAnimations(String file, Model model) {
        logger.info("Loading animations from '{}'", file);

        AIScene aiScene = importScene(file, new Importer.Options(false, false));
        List<Animation> animations = new ArrayList<>();

        loadAnimations(aiScene, model.getBones(), animations);

        return animations;
    }

    public static void loadModel(Model.Builder builder) throws IOException {
//        aiAttachLogStream(AILogStream.create() //TODO i mean... it's possible if ever needed
//                .callback((messagePointer, userPointer) -> {
//                    String message = MemoryUtil
//                            .memUTF8(messagePointer)
//                            .replace(System.lineSeparator(), "") //TODO assimp appears to always spit out unix lfs; so the below line may suffice
//                            .replace("\n", "");
//                    logger.info("Assimp: {}", message);
//                }));

        AIScene aiScene = importScene(
                builder.getFile(),
                new Importer.Options(builder.shouldFlipUVs(), builder.shouldFlipWinding())
        );

        AINode rootNode = aiScene.mRootNode();

        if ((aiScene.mFlags() & AI_SCENE_FLAGS_INCOMPLETE) != 0
                || rootNode == null) {
            throw new IOException(aiGetErrorString());
        }

        loadEmbeddedTextures(aiScene, builder.getEmbeddedTextures());
        loadMaterials(aiScene, builder, builder.getEmbeddedTextures());
        loadMeshes(aiScene, builder);
        processNode(null, rootNode, builder);
        loadAnimations(aiScene, builder.getBones(), builder.getAnimations()); //animations reference bones, which need first be loaded from the meshes, and also require the nodes to resolve the back reference
        calculateModelBoundingBox(builder);

        aiReleaseImport(aiScene);

        logger.debug("Loaded model {} with {} node{}, {} mesh{}, {} material{}, {} bone{} and {} animation{}",
                builder.getName(),
                builder.getNodes().size(), builder.getNodes().size() == 1 ? "" : "s",
                builder.getMeshes().size(), builder.getMeshes().size() == 1 ? "" : "es",
                builder.getMaterials().size(), builder.getMaterials().size() == 1 ? "" : "s",
                builder.getBones().size(), builder.getBones().size() == 1 ? "" : "s",
                builder.getAnimations().size(), builder.getAnimations().size() == 1 ? "" : "s");
    }

    private static void processNode(Node parent, AINode aiNode, Model.Builder builder) {
        String nodeName = aiNode.mName().dataString();

        Matrix4fc transformationMatrix = AssimpUtils.fromAI(aiNode.mTransformation());
        Transform transform = Transform.fromMatrix4f(transformationMatrix);

        Bone bone = builder.getBones().stream()
                .filter(_bone -> _bone.name().equals(nodeName))
                .findAny()
                .orElse(null);

        Node node = new Node(
                nodeName,
                parent,
                transform,
                getNodeMeshes(aiNode, builder),
                bone
        );

        builder.getNodes().add(node);
        if (parent != null)
            parent.getChildren().add(node);

        PointerBuffer mChildren = aiNode.mChildren();
        if (mChildren == null) return;
        for (int i = 0; i < aiNode.mNumChildren(); i++)
            processNode(node, AINode.create(mChildren.get()), builder);
    }

    private static List<Mesh> getNodeMeshes(AINode aiNode, Model.Builder builder) {
        IntBuffer meshIndexBuffer = aiNode.mMeshes();
        if (meshIndexBuffer == null)
            return List.of();

        return Stream
                .generate(meshIndexBuffer::get)
                .limit(aiNode.mNumMeshes())
                .map(builder.getMeshes()::get)
                .toList();
    }

    private static void calculateModelBoundingBox(Model.Builder builder) {
        Vector3f min = new Vector3f(), max = new Vector3f();
        for (Mesh mesh : builder.getMeshes()) {
            mesh.getBoundingBox().getMin().min(min, min);
            mesh.getBoundingBox().getMax().max(max, max);
        }
        builder.setBoundingBox(new AABB(min, max));
    }

}
