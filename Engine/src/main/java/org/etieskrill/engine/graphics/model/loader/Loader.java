package org.etieskrill.engine.graphics.model.loader;

import org.etieskrill.engine.entity.data.AABB;
import org.etieskrill.engine.graphics.model.Mesh;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.Node;
import org.etieskrill.engine.graphics.util.AssimpUtils;
import org.etieskrill.engine.util.ResourceReader;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIFile;
import org.lwjgl.assimp.AIFileIO;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.stream.Stream;

import static org.etieskrill.engine.config.ResourcePaths.MODEL_PATH;
import static org.etieskrill.engine.graphics.model.loader.AnimationLoader.loadAnimations;
import static org.etieskrill.engine.graphics.model.loader.MaterialLoader.loadEmbeddedTextures;
import static org.etieskrill.engine.graphics.model.loader.MaterialLoader.loadMaterials;
import static org.etieskrill.engine.graphics.model.loader.MeshProcessor.loadMeshes;
import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memCopy;

public class Loader {

    private static final Logger logger = LoggerFactory.getLogger(Loader.class);

    public static void loadModel(Model.Builder builder) throws IOException {

        //TODO properly set these
        int processFlags =
                aiProcess_Triangulate |
//                        aiProcess_SortByPType |
                        (builder.shouldFlipUVs() ? aiProcess_FlipUVs : 0) |
//                        aiProcess_OptimizeMeshes |
//                        aiProcess_OptimizeGraph |
//                        aiProcess_JoinIdenticalVertices |
//                        aiProcess_RemoveRedundantMaterials |
//                        aiProcess_FindInvalidData |
//                        aiProcess_GenUVCoords |
//                        aiProcess_TransformUVCoords |
//                        aiProcess_FindInstances |
//                        aiProcess_PreTransformVertices | //TODO add ~static~ flag or smth, but none of this baby shit anymore
                        (builder.shouldFlipWinding() ? aiProcess_FlipWindingOrder : 0);

        AIFileIO fileIO = AIFileIO.create().OpenProc((pFileIO, fileName, openMode) -> {
            String name = MemoryUtil.memUTF8(fileName);
            ByteBuffer buffer = ResourceReader.getRawClassPathResource(MODEL_PATH + name);

            AIFile aiFile = AIFile.create().ReadProc((pFile, pBuffer, size, count) -> {
                long blocksRead = Math.min(buffer.remaining() / size, count);
                memCopy(memAddress(buffer), pBuffer, blocksRead * size);
                buffer.position((int) (buffer.position() + (blocksRead * size)));
                return blocksRead;
            }).SeekProc((pFile, offset, origin) -> {
                switch (origin) {
                    case aiOrigin_SET -> buffer.position((int) offset);
                    case aiOrigin_CUR -> buffer.position(buffer.position() + (int) offset);
                    case aiOrigin_END -> buffer.position(buffer.limit() + (int) offset);
                }
                return 0;
            }).FileSizeProc(pFile -> buffer.limit());

            return aiFile.address();
        }).CloseProc((pFileIO, pFile) -> {
            AIFile aiFile = AIFile.create(pFile);
            aiFile.ReadProc().free();
            aiFile.SeekProc().free();
            aiFile.FileSizeProc().free();
            //TODO shouldn't the buffer from OpenProc also be freed here?
        });
        AIScene scene = aiImportFileEx(builder.getFile(), processFlags, fileIO);
        if (scene == null || (scene.mFlags() & AI_SCENE_FLAGS_INCOMPLETE) != 0
                || scene.mRootNode() == null) {
            throw new IOException(aiGetErrorString());
        }

        loadEmbeddedTextures(scene, builder.getEmbeddedTextures());
        loadMaterials(scene, builder, builder.getEmbeddedTextures());
        loadMeshes(scene, builder);
        processNode(null, scene.mRootNode(), builder);
        loadAnimations(scene, builder); //animations reference bones, which need first be loaded from the nodes
        calculateModelBoundingBox(builder);

        aiReleaseImport(scene);

        logger.debug("Loaded model {} with {} node{}, {} mesh{} and {} material{}", builder.getName(),
                builder.getNodes().size(), builder.getNodes().size() == 1 ? "" : "s",
                builder.getMeshes().size(), builder.getMeshes().size() == 1 ? "" : "es",
                builder.getMaterials().size(), builder.getMaterials().size() == 1 ? "" : "s");
    }

    private static void processNode(Node parent, AINode aiNode, Model.Builder builder) {
        if (builder.getNodes().size() < 10) logger.info("New node '{}' was loaded", aiNode.mName().dataString());
        Node node = new Node(
                aiNode.mName().dataString(),
                AssimpUtils.fromAI(aiNode.mTransformation()),
                getNodeMeshes(aiNode, builder)
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
