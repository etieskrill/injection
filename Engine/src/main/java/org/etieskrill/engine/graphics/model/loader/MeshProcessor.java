package org.etieskrill.engine.graphics.model.loader;

import org.etieskrill.engine.entity.data.AABB;
import org.etieskrill.engine.graphics.model.Bone;
import org.etieskrill.engine.graphics.model.Material;
import org.etieskrill.engine.graphics.model.Mesh;
import org.etieskrill.engine.graphics.model.Vertex;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIVector3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.etieskrill.engine.graphics.model.loader.AnimationLoader.getBones;
import static org.lwjgl.assimp.Assimp.*;

class MeshProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MeshProcessor.class);

    static Mesh processMesh(AIMesh aiMesh, List<Material> materials) {
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
            vertices.add(new Vertex.Builder(positions.get(i))
                    .normal(!normals.isEmpty() ? normals.get(i) : null)
                    .textureCoords(!texCoords.isEmpty() ? texCoords.get(i) : null)
                    .build()
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

}
