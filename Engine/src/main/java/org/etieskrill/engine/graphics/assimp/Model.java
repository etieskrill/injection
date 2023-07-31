package org.etieskrill.engine.graphics.assimp;

import org.etieskrill.engine.graphics.gl.Loader;
import org.etieskrill.engine.graphics.gl.Texture;
import org.etieskrill.engine.graphics.gl.Texture.TextureType;
import org.etieskrill.engine.math.Vec2f;
import org.etieskrill.engine.math.Vec3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Vector;

import static org.lwjgl.assimp.Assimp.*;

public class Model {
    
    private static final String directory = "Engine/src/main/resources/models/";
    
    private static final Loader loader = Loader.get();
    
    private final Vector<Mesh> meshes;
    private final Vector<Material> materials;
    
    public Model(String file) {
        this.meshes = new Vector<>();
        this.materials = new Vector<>();
        loadModel(file);
    }
    
    private void loadModel(String file) {
        AIScene scene = aiImportFile(directory + file,
                aiProcess_Triangulate | aiProcess_FlipUVs); //| aiProcess_OptimizeMeshes);
    
        if (scene == null || (scene.mFlags() & AI_SCENE_FLAGS_INCOMPLETE) != 0
                || scene.mRootNode() == null) {
            System.err.println(aiGetErrorString());
            return;
        }
    
        PointerBuffer mMaterials = scene.mMaterials();
        if (mMaterials == null) return;
        for (int i = 0; i < scene.mNumMaterials(); i++)
            materials.add(processMaterial(AIMaterial.create(mMaterials.get())));
        
        processNode(scene.mRootNode(), scene);
    }
    
    private void processNode(AINode node, AIScene scene) {
        PointerBuffer mMeshes = scene.mMeshes();
        if (mMeshes == null) return;
        for (int i = 0; i < node.mNumMeshes(); i++) {
            meshes.add(processMesh(AIMesh.create(mMeshes.get(node.mMeshes().get(i))), scene));
        }
        
        PointerBuffer mChildren = node.mChildren();
        if (mChildren == null) return;
        for (int i = 0; i < node.mNumChildren(); i++)
            processNode(AINode.create(mChildren.get()), scene);
    }
    
    private Mesh processMesh(AIMesh mesh, AIScene scene) {
        Vector<Vec3f> positions = new Vector<>();
        mesh.mVertices().forEach(vertex -> positions.add(new Vec3f(vertex.x(), vertex.y(), vertex.z())));
        
        Vector<Vec3f> normals = new Vector<>();
        if (mesh.mNormals() != null)
            mesh.mNormals().forEach(normal -> normals.add(new Vec3f(normal.x(), normal.y(), normal.z())));
        
        Vector<Vec2f> texCoords = new Vector<>();
        if (mesh.mTextureCoords(0) != null)
            mesh.mTextureCoords(0).forEach(texCoord -> texCoords.add(new Vec2f(texCoord.x(), texCoord.y())));
        
        Vector<Vertex> vertices = new Vector<>();
        for (int i = 0; i < mesh.mNumVertices(); i++)
            vertices.add(new Vertex(positions.get(i), normals.get(0), texCoords.get(0)));
        
        Vector<Short> indices = new Vector<>();
        for (int i = 0; i < mesh.mNumFaces(); i++) {
            AIFace face = mesh.mFaces().get(i);
            IntBuffer buffer = face.mIndices();
            for (int j = 0; j < face.mNumIndices(); j++)
                indices.add((short) buffer.get());
        }
    
        AIMaterial aiMat = AIMaterial.create(scene.mMaterials().get(mesh.mMaterialIndex()));
        System.out.println(aiMat.mNumProperties() + " " + aiMat.mNumAllocated());
    
        Material material = materials.get(mesh.mMaterialIndex());
        
        return loader.loadToVAO(vertices, indices, material);
    }
    
    private Material processMaterial(AIMaterial aiMaterial) {
        Material material = new Material();
        
        PointerBuffer buffer = aiMaterial.mProperties();
        for (int i = 0; i < aiMaterial.mNumProperties(); i++) {
            AIMaterialProperty property = AIMaterialProperty.create(buffer.get());
            
            TextureType type = TextureType.UNKNOWN;
            switch (property.mType()) {
                case aiTextureType_DIFFUSE -> type = TextureType.DIFFUSE;
                case aiTextureType_SPECULAR -> type = TextureType.SPECULAR;
                case aiTextureType_EMISSIVE -> type = TextureType.EMISSIVE;
                case aiTextureType_NONE -> {
                    System.err.println("what the fuck do i do here huh");
                    continue;
                }
            };
            
//            ByteBuffer fileBuffer = property.mData();
//            for (int j = 0; j < property.mDataLength(); j++) {
//                System.out.print((char) fileBuffer.get());
//            }
//            System.out.println();
            
            StringBuilder builder = new StringBuilder();
            ByteBuffer fileBuffer = property.mData();
            for (int j = 0; j < property.mDataLength(); j++) {
                builder.append((char) fileBuffer.get());
            }
            System.out.println(builder);
    
            for (int j = 0; j < 22; j++) {
                System.out.print(aiGetMaterialTextureCount(aiMaterial, i) + " ");
            }
            System.out.println();
            
            //String file = new String(Arrays.copyOfRange(property.mData().array(), 0, property.mDataLength()));
            //material.addTexture(loader.loadTexture(file, "backpack_" + type.name().toLowerCase(), type));
        }
        
        return material;
    }
    
    public Vector<Mesh> getMeshes() {
        return meshes;
    }
    
}
