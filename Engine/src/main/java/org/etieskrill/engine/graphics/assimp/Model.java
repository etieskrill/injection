package org.etieskrill.engine.graphics.assimp;

import glm.mat._4.Mat4;
import org.etieskrill.engine.graphics.gl.Loader;
import org.etieskrill.engine.graphics.gl.Texture;
import org.etieskrill.engine.graphics.gl.Texture.TextureType;
import org.etieskrill.engine.math.Vec2f;
import org.etieskrill.engine.math.Vec3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

import java.nio.IntBuffer;
import java.util.Vector;

import static org.lwjgl.assimp.Assimp.*;

public class Model {
    
    private static final String directory = "Engine/src/main/resources/models/";
    
    private static final Loader loader = Loader.get();
    
    private final Vector<Mesh> meshes;
    private final Vector<Material> materials;
    
    private final String name;
    
    public Model(String file) {
        this.meshes = new Vector<>();
        this.materials = new Vector<>();
        this.name = file.split("\\.")[0];
        loadModel(file);
    }
    
    private void loadModel(String file) {
        AIScene scene = aiImportFile(directory + file,
                aiProcess_Triangulate | aiProcess_FlipUVs | aiProcess_OptimizeMeshes);
    
        if (scene == null || (scene.mFlags() & AI_SCENE_FLAGS_INCOMPLETE) != 0
                || scene.mRootNode() == null) {
            System.err.println(aiGetErrorString());
            return;
        }
    
        PointerBuffer mMaterials = scene.mMaterials();
        if (mMaterials == null) return;
        for (int i = 0; i < scene.mNumMaterials(); i++) {
            materials.add(processMaterial(AIMaterial.create(mMaterials.get())));
        }
        
        processNode(scene.mRootNode(), scene);
    }
    
    private void processNode(AINode node, AIScene scene) {
        PointerBuffer mMeshes = scene.mMeshes();
        if (mMeshes == null) return;
        for (int i = 0; i < node.mNumMeshes(); i++) {
            Mat4 transform = toMat4(node.mTransformation());
            AINode parent = node;
            while ((parent = parent.mParent()) != null) {
                transform.mul(toMat4(parent.mTransformation()));
            }
            meshes.add(processMesh(AIMesh.create(mMeshes.get(node.mMeshes().get(i))), transform));
        }
        
        PointerBuffer mChildren = node.mChildren();
        if (mChildren == null) return;
        for (int i = 0; i < node.mNumChildren(); i++)
            processNode(AINode.create(mChildren.get()), scene);
    }
    
    private Mesh processMesh(AIMesh mesh, Mat4 transform) {
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
        
        Material material = materials.get(mesh.mMaterialIndex());
        
        return loader.loadToVAO(vertices, indices, material, transform);
    }
    
    private Material processMaterial(AIMaterial aiMaterial) {
        Material material = new Material();
    
        /* this is for ... debugging 3d model file structures, which should not be my problem in the first place
        for (int i = 0; i < 22; i++) {
            int count = aiGetMaterialTextureCount(aiMaterial, i);
            System.out.print("[" + i + "] " + (count > 0 ? "!" + count + "!" : count) + " ");
        }
        System.out.println();
        */
        
        addTexturesToMaterial(material, aiMaterial, aiTextureType_DIFFUSE);
        //addTexturesToMaterial(material, aiMaterial, aiTextureType_SHININESS);
        
//        AIString.Buffer string = AIString.malloc(256);
//        System.err.println(aiGetMaterialTexture(aiMaterial, aiTextureType_DIFFUSE, 0, AIString.create(string.address()), new int[1], null, null, null, null, null));
//        System.err.println("data: " + string.dataString());
        
        /*PointerBuffer buffer = aiMaterial.mProperties();
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
            System.out.println(i + " " + property.mType() + " " + builder);
    
            int k = 0;
            for (int j = 0; j < 22; j++) {
                int textures = aiGetMaterialTextureCount(aiMaterial, i);
                k += textures;
                System.out.print(textures + " ");
            }
            System.out.println(k);
            
            //String file = new String(Arrays.copyOfRange(property.mData().array(), 0, property.mDataLength()));
            //material.addTexture(loader.loadTexture(file, "backpack_" + type.name().toLowerCase(), type));
        }*/
        
        return material;
    }
    
    private void addTexturesToMaterial(Material material, AIMaterial aiMaterial, int textureType) {
        AIString file = AIString.create();
        
        for (int i = 0; i < aiGetMaterialTextureCount(aiMaterial, textureType); i++) {
            if (aiGetMaterialTexture(aiMaterial, textureType, i, file,
                    new int[1], null, null, null, null, null)
                    != aiReturn_SUCCESS) {
                System.err.println(aiGetErrorString());
                continue;
            }
            
            TextureType type = switch (textureType) {
                case aiTextureType_DIFFUSE -> TextureType.DIFFUSE;
                case aiTextureType_SPECULAR -> TextureType.SPECULAR;
                case aiTextureType_EMISSIVE -> TextureType.EMISSIVE;
                default -> TextureType.UNKNOWN;
            };
            
            Texture texture = loader.loadTexture(file.dataString(), name + type.name().toLowerCase() + i, type);
            material.addTexture(texture);
        }
    }
    
    public Vector<Mesh> getMeshes() {
        return meshes;
    }
    
    private static Mat4 toMat4(AIMatrix4x4 mat) {
        float[] values = new float[] {
                mat.a1(), mat.a2(), mat.a3(), mat.a4(),
                mat.b1(), mat.b2(), mat.b4(), mat.b4(),
                mat.c1(), mat.c2(), mat.c3(), mat.a4(),
                mat.d1(), mat.d2(), mat.d3(), mat.a4()
        };
        return new Mat4(values); //TODO transpose if some weird fuckshit happens
    }
    
}
