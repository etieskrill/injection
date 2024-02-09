package org.etieskrill.engine.graphics.model.loader;

import org.etieskrill.engine.entity.data.AABB;
import org.etieskrill.engine.graphics.model.*;
import org.etieskrill.engine.util.ResourceReader;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.etieskrill.engine.config.ResourcePaths.MODEL_PATH;
import static org.etieskrill.engine.graphics.model.loader.AnimationLoader.getBones;
import static org.etieskrill.engine.graphics.model.loader.AnimationLoader.loadAnimations;
import static org.etieskrill.engine.graphics.model.loader.MaterialLoader.loadEmbeddedTextures;
import static org.etieskrill.engine.graphics.model.loader.MaterialLoader.loadMaterials;
import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memCopy;

public class Loader {

    private static final Logger logger = LoggerFactory.getLogger(Loader.class);

    public static void loadModel(Model.Builder builder) throws IOException {

        //TODO properly set these
        int processFlags =
                aiProcess_Triangulate |
                        aiProcess_SortByPType |
                        (builder.shouldFlipUVs() ? aiProcess_FlipUVs : 0) |
                        aiProcess_OptimizeMeshes |
                        aiProcess_OptimizeGraph |
                        aiProcess_JoinIdenticalVertices |
                        aiProcess_RemoveRedundantMaterials |
                        aiProcess_FindInvalidData |
                        aiProcess_GenUVCoords |
                        aiProcess_TransformUVCoords |
                        aiProcess_FindInstances |
                        aiProcess_PreTransformVertices |
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
        processNode(scene.mRootNode(), scene, builder);
        loadAnimations(scene, builder.getAnimations(), builder.getMeshes()); //animations reference bones, which need first be loaded from the nodes
        calculateModelBoundingBox(builder);

        aiReleaseImport(scene);

        logger.debug("Loaded model {} with {} mesh{} and {} material{}", builder.getName(),
                builder.getMeshes().size(), builder.getMeshes().size() == 1 ? "" : "es",
                builder.getMaterials().size(), builder.getMaterials().size() == 1 ? "" : "s");
    }

    private static void processNode(AINode node, AIScene scene, Model.Builder builder) {
        PointerBuffer mMeshes = scene.mMeshes();
        if (mMeshes == null) return;
        for (int i = 0; i < node.mNumMeshes(); i++)
            builder.getMeshes().add(processMesh(AIMesh.create(mMeshes.get(node.mMeshes().get(i))), builder.getMaterials()));

        PointerBuffer mChildren = node.mChildren();
        if (mChildren == null) return;
        for (int i = 0; i < node.mNumChildren(); i++)
            processNode(AINode.create(mChildren.get()), scene, builder);
    }

    private static Mesh processMesh(AIMesh aiMesh, List<Material> materials) {
        //TODO add AABB loading but oh FUCK each goddamn mesh has a separate one FFS

        int numVertices = aiMesh.mNumVertices();
        List<Vector3fc> positions = new ArrayList<>(numVertices);
        aiMesh.mVertices().forEach(vertex -> positions.add(new Vector3f(vertex.x(), vertex.y(), vertex.z())));

        List<Vector3fc> normals = new ArrayList<>(numVertices);
        if (aiMesh.mNormals() != null)
            aiMesh.mNormals().forEach(normal -> normals.add(new Vector3f(normal.x(), normal.y(), normal.z())));

        List<Vector2fc> texCoords = new ArrayList<>(numVertices);
        if (aiMesh.mTextureCoords(0) != null)
            aiMesh.mTextureCoords(0).forEach(texCoord -> texCoords.add(new Vector2f(texCoord.x(), texCoord.y())));

        List<Vertex> vertices = new ArrayList<>(numVertices);
        for (int i = 0; i < aiMesh.mNumVertices(); i++)
            vertices.add(new Vertex(
                    positions.get(i),
                    normals.size() > 0 ? normals.get(i) : new Vector3f(),
                    texCoords.size() > 0 ? texCoords.get(i) : new Vector2f())
            );

        //three because a face is usually a triangle, but this list is discarded at the first opportunity a/w
        //TODO add loader versions which transmit the minimal amount of data (shorts for indices, smaller vectors)
        List<Integer> indices = new ArrayList<>(aiMesh.mNumFaces() * 3);
        for (int i = 0; i < aiMesh.mNumFaces(); i++) {
            AIFace face = aiMesh.mFaces().get(i);
            IntBuffer buffer = face.mIndices();
            for (int j = 0; j < face.mNumIndices(); j++)
                indices.add(buffer.get());
        }

        List<Bone> bones = getBones(aiMesh, vertices);

        AIVector3D min = aiMesh.mAABB().mMin();
        AIVector3D max = aiMesh.mAABB().mMax();
        AABB boundingBox = new AABB(new Vector3f(min.x(), min.y(), min.z()),
                new Vector3f(max.x(), max.y(), max.z()));

        if (boundingBox.getMin().equals(new Vector3f(), 0.00001f)
                || boundingBox.getMax().equals(new Vector3f(), 0.00001f))
            boundingBox = calculateBoundingBox(vertices);

        Material material = materials.get(aiMesh.mMaterialIndex());

        Mesh.DrawMode drawMode = switch (aiMesh.mPrimitiveTypes()) {
            case aiPrimitiveType_POINT -> Mesh.DrawMode.POINTS;
            case aiPrimitiveType_LINE -> Mesh.DrawMode.LINES;
            case aiPrimitiveType_TRIANGLE -> Mesh.DrawMode.TRIANGLES;
            default -> {
                logger.warn("Cannot draw primitives of type 0x{}, using default of TRIANGLES",
                        Integer.toHexString(aiMesh.mPrimitiveTypes()));
                yield Mesh.DrawMode.TRIANGLES;
            }
        };

        Mesh ret = MeshLoader.loadToVAO(vertices, indices, material, bones, boundingBox, drawMode);

        logger.trace("Loaded mesh with {} vertices and {} indices, {} normals, {} uv coordinates", vertices.size(),
                indices.size(), !normals.isEmpty() ? "with" : "without", !texCoords.isEmpty() ? "with" : "without");
        return ret;
    }

    private static AABB calculateBoundingBox(List<Vertex> vertices) {
        float minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;

        //for models as complicated as the skeleton, a stream variant presents a slight improvement in performance,
        //measurable in the single milliseconds, which is not worth the cost of initialising a stream for a model with
        //very few vertices
        for (Vertex vertex : vertices) {
            Vector3fc pos = vertex.getPosition();
            minX = Math.min(pos.x(), minX);
            minY = Math.min(pos.y(), minY);
            minZ = Math.min(pos.z(), minZ);
            maxX = Math.max(pos.x(), maxX);
            maxY = Math.max(pos.y(), maxY);
            maxZ = Math.max(pos.z(), maxZ);
        }

        return new AABB(new Vector3f(minX, minY, minZ), new Vector3f(maxX, maxY, maxZ));
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
