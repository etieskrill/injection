package org.etieskrill.engine.graphics.model.loader;

import org.etieskrill.engine.entity.component.AABB;
import org.etieskrill.engine.graphics.model.*;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.etieskrill.engine.graphics.model.loader.AnimationLoader.getBones;
import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.util.meshoptimizer.MeshOptimizer.meshopt_simplify;

public class MeshProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MeshProcessor.class);

    static void loadMeshes(AIScene scene, Model.Builder builder) {
        PointerBuffer meshBuffer = scene.mMeshes();
        if (meshBuffer == null) return;

        List<Mesh> meshes = Stream
                .generate(meshBuffer::get)
                .limit(scene.mNumMeshes())
                .map(AIMesh::create)
                .map(aiMesh -> processMesh(aiMesh, builder.getMaterials()))
                .toList();
        builder.getMeshes().addAll(meshes);
        builder.getBones().addAll(meshes.stream()
                .flatMap(mesh -> mesh.getBones().stream())
                .distinct()
                .toList());
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

        List<Vector3fc> tangents = new ArrayList<>(numVertices);
        List<Vector3fc> biTangents = new ArrayList<>(numVertices);
        if (aiMesh.mTangents() != null) {
            aiMesh.mTangents().forEach(tangent -> tangents.add(new Vector3f(tangent.x(), tangent.y(), tangent.z())));
            aiMesh.mBitangents().forEach(bitangent -> biTangents.add(new Vector3f(bitangent.x(), bitangent.y(), bitangent.z())));
        }

        List<Vertex.VertexBuilder> vertexBuilders = new ArrayList<>(numVertices);
        for (int i = 0; i < aiMesh.mNumVertices(); i++) vertexBuilders.add(Vertex.builder(positions.get(i)));
        if (!normals.isEmpty()) {
            for (int i = 0; i < aiMesh.mNumVertices(); i++)
                vertexBuilders.get(i).normal(normals.get(i));
        }
        if (!texCoords.isEmpty()) {
            for (int i = 0; i < aiMesh.mNumVertices(); i++)
                vertexBuilders.get(i).textureCoords(texCoords.get(i));
        }
        if (!tangents.isEmpty()) {
            for (int i = 0; i < aiMesh.mNumVertices(); i++)
                vertexBuilders.get(i).tangent(tangents.get(i)).biTangent(biTangents.get(i));
        }

        //three because a face is usually a triangle, but this list is discarded at the first opportunity a/w
        //TODO add loader versions which transmit the minimal amount of data (shorts for indices, smaller vectors)
        boolean calcTangentsWarning = false;
        List<Integer> indices = new ArrayList<>(aiMesh.mNumFaces() * 3);
        for (int i = 0; i < aiMesh.mNumFaces(); i++) {
            AIFace face = aiMesh.mFaces().get(i);
            IntBuffer buffer = face.mIndices();

//            boolean calcTangents = face.mNumIndices() == 3;
//            if (!calcTangentsWarning && !calcTangents) {
//                logger.warn("Primitive type with {} vertices will not have tangents calculated", face.mNumIndices());
//                calcTangentsWarning = true;
//            }
//
//            Vertex.Builder vertex1 = null, vertex2 = null, vertex3 = null;
            for (int j = 0; j < face.mNumIndices(); j++) {
                int index = buffer.get();
                indices.add(index);
//
//                if (!calcTangents) continue;
//                switch (j) {
//                    case 0 -> vertex1 = vertexBuilders.get(index);
//                    case 1 -> vertex2 = vertexBuilders.get(index);
//                    case 2 -> vertex3 = vertexBuilders.get(index);
//                }
            }
//
//            Vector3fc edge1 = vertex2.position().sub(vertex1.position(), new Vector3f());
//            Vector3fc edge2 = vertex3.position().sub(vertex1.position(), new Vector3f());
//            Vector2fc deltaTex1 = vertex2.textureCoords().sub(vertex1.textureCoords(), new Vector2f());
//            Vector2fc deltaTex2 = vertex3.textureCoords().sub(vertex1.textureCoords(), new Vector2f());
//
//            Vector3f tangent = new Vector3f();
//            Vector3f biTangent = new Vector3f();
//
//            float f = 1.0f / (deltaTex1.x() * deltaTex2.y() - deltaTex2.x() * deltaTex1.y());
//
//            tangent.x = f * (deltaTex2.y() * edge1.x() - deltaTex1.y() * edge2.x());
//            tangent.y = f * (deltaTex2.y() * edge1.y() - deltaTex1.y() * edge2.y());
//            tangent.z = f * (deltaTex2.y() * edge1.z() - deltaTex1.y() * edge2.z());
//
//            biTangent.x = f * (-deltaTex2.x() * edge1.x() + deltaTex1.x() * edge2.x());
//            biTangent.y = f * (-deltaTex2.x() * edge1.y() + deltaTex1.x() * edge2.y());
//            biTangent.z = f * (-deltaTex2.x() * edge1.z() + deltaTex1.x() * edge2.z());
//
//            vertex1.tangent(tangent).biTangent(biTangent);
//            vertex2.tangent(tangent).biTangent(biTangent);
//            vertex3.tangent(tangent).biTangent(biTangent);
        }

        List<Bone> bones = getBones(aiMesh, vertexBuilders);

        List<Vertex> vertices = vertexBuilders.stream()
                .map(Vertex.VertexBuilder::build)
                .toList();

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

    public static void optimiseMesh(Mesh mesh, int targetIndexCount, float maxDeformation) {
        if (!mesh.getVao().isIndexed()) {
            throw new IllegalArgumentException("Can only optimise indexed meshes"); //TODO workaround: temporary 1-1 vertex-index buffer? duplicate vertices did make the algorithm shit itself tho, so probably do an actual index run using... assimp? can it even do that?
        }

        ByteBuffer vertexData = mesh.getVao().getVertexBuffer().getData();
        IntBuffer indexData = mesh.getVao().getIndexBuffer().getData().asIntBuffer();

        ByteBuffer newIndexData = BufferUtils.createByteBuffer(Integer.BYTES * indexData.capacity());

        FloatBuffer errorBuffer = BufferUtils.createFloatBuffer(1);
        int vertexBytes = VertexAccessor.getInstance().getElementByteSize();
        long numIndices = meshopt_simplify(newIndexData.asIntBuffer(), indexData, vertexData.asFloatBuffer(),
                vertexData.capacity() / vertexBytes, vertexBytes,
                targetIndexCount, maxDeformation, 0, errorBuffer);
        //TODO compress vertex buffer using new indices

        logger.trace("Original mesh has {} vertices and {} indices",
                vertexData.capacity() / vertexBytes, indexData.capacity());
        logger.trace("Optimised mesh has {} indices and a deformation of {}% (max {}%)", numIndices,
                "%5.3f".formatted(100 * errorBuffer.get()), "%5.3f".formatted(100 * maxDeformation));
        logger.debug("Mesh was compressed by a factor of {}",
                "%4.1f".formatted((float) indexData.capacity() / numIndices));

        mesh.getVao().getIndexBuffer().setData(newIndexData);
        mesh.setNumIndices((int) numIndices);
    }

}
