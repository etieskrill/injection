package org.etieskrill.engine.graphics.model.loader;

import lombok.extern.slf4j.Slf4j;
import org.etieskrill.engine.entity.component.AABB;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.graphics.animation.Animation;
import org.etieskrill.engine.graphics.animation.BoneMatcher;
import org.etieskrill.engine.graphics.model.Bone;
import org.etieskrill.engine.graphics.model.Mesh;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.Node;
import org.etieskrill.engine.graphics.util.AssimpUtils;
import org.etieskrill.engine.time.StepTimer;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AIScene;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;
import static org.etieskrill.engine.graphics.model.loader.AnimationLoader.loadAnimations;
import static org.etieskrill.engine.graphics.model.loader.Importer.importScene;
import static org.etieskrill.engine.graphics.model.loader.MaterialLoader.loadEmbeddedTextures;
import static org.etieskrill.engine.graphics.model.loader.MaterialLoader.loadMaterials;
import static org.etieskrill.engine.graphics.model.loader.MeshProcessor.loadMeshes;
import static org.lwjgl.assimp.Assimp.*;

@Slf4j
public class Loader {

    public static final BoneMatcher DEFAULT_BONE_MATCHER = (modelBone, animBone) -> {
        //TODO pretty lenient bone name matching for the time being to allow for several file formats - should be undone
        return modelBone.replace("_", "").replace(":", "")
                .equals(animBone.replace("_", "").replace(":", ""));
    };

    private static final StepTimer timer = new StepTimer(logger);

    //TODO loading animations is probs to be separated from model loading: either
    // - load bones with animation every time, and resolve stuff when assigning an animation to a model/animator or
    // - require model/nodes when loading in order to link dependencies directly on load
    public static List<Animation> loadModelAnimations(String file, Model model, BoneMatcher boneMatcher) {
        logger.info("Loading animations from '{}'", file);

        AIScene aiScene = importScene(file, new Importer.Options(false, false));
        List<Animation> animations = new ArrayList<>();

        loadAnimations(aiScene, model.getBones(), animations, boneMatcher);

        return animations;
    }

    public static List<Animation> loadModelAnimations(String file, Model model) {
        return loadModelAnimations(file, model, DEFAULT_BONE_MATCHER);
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

        timer.start();

        AIScene aiScene = importScene(
                builder.getFile(),
                new Importer.Options(builder.shouldFlipUVs(), builder.shouldFlipWinding())
        );

        timer.log("Import");

        AINode rootNode = aiScene.mRootNode();

        if ((aiScene.mFlags() & AI_SCENE_FLAGS_INCOMPLETE) != 0
                || rootNode == null) {
            throw new IOException(aiGetErrorString());
        }

        loadEmbeddedTextures(aiScene, builder.getEmbeddedTextures());
        timer.log("Embedded");
        loadMaterials(aiScene, builder);
        timer.log("Materials");
        loadMeshes(aiScene, builder);
        timer.log("Meshes");
        processNode(null, rootNode, builder);
        loadAnimations(aiScene, builder.getBones(), builder.getAnimations(), DEFAULT_BONE_MATCHER); //animations reference bones, which need first be loaded from the meshes, and also require the nodes to resolve the back reference
        timer.log("Animations");
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
                .collect(toCollection(ArrayList::new));
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
