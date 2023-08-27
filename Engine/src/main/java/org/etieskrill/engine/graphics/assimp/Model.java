package org.etieskrill.engine.graphics.assimp;

import glm_.mat4x4.Mat4;
import glm_.vec2.Vec2;
import glm_.vec3.Vec3;
import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.graphics.gl.Loaders.TextureLoader;
import org.etieskrill.engine.graphics.gl.Texture;
import org.etieskrill.engine.graphics.gl.Texture.TextureType;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Vector;
import java.util.function.Supplier;

import static org.lwjgl.assimp.Assimp.*;

public class Model implements Disposable {
    
    private static final String DIRECTORY = "Engine/src/main/resources/models/";
    private static final Supplier<Model> ERROR_MODEL = () -> Model.ofFile("cube.obj");
    
    private static final Logger logger = LoggerFactory.getLogger(Model.class);
    
    private final Vector<Mesh> meshes; //TODO these should become immutable after model instantiation
    private final Vector<Material> materials; //TODO since meshes know their materials, these here may not be necessary?
    
    private final String file;
    private final String name;
    
    private final Vec3 position;
    private final Vec3 scale;
    private float rotation;
    private final Vec3 rotationAxis;
    
    private final Mat4 transform;
    
    public static Model ofFile(String file) {
        return ofFile(file, file.split("\\.")[0]);
    }
    
    public static Model ofFile(String file, String name) {
        if (file.isBlank()) throw new IllegalArgumentException("File name cannot be blank");
        if (file.contains("/")) throw new IllegalArgumentException("Custom folder structure not implemented yet: " + file);
    
        try {
            return new Model(file, name, new Vector<>(), new Vector<>(),
                    new Vec3(0f), new Vec3(1f), 0f, new Vec3(0f), new Mat4().identity());
        } catch (IOException e) {
            logger.debug("Exception while loading model, using default: ", e);
            return ERROR_MODEL.get();
        }
    }
    
    /**
     * Copy constructor, for ... not cloning.
     */
    public Model(Model model) {
        //TODO since the below three lines represent the model as loaded into the graphics memory
        // and should effectively be immutable, consider encapsulating them into another class
        this.meshes = model.meshes;
        this.materials = model.materials;
        this.file = model.file;
        this.name = model.name;
        logger.trace("Creating copy of model {}", name);
        this.position = new Vec3(model.position);
        this.scale = new Vec3(model.scale);
        this.rotation = model.rotation;
        this.rotationAxis = new Vec3(model.rotationAxis);
        this.transform = new Mat4(model.transform);
    }
    
    private Model(String file, String name, Vector<Mesh> meshes, Vector<Material> materials,
                 Vec3 position, Vec3 scale, float rotation, Vec3 rotationAxis, Mat4 transform) throws IOException {
        this.meshes = meshes;
        this.materials = materials;
        this.file = file.split("\\.")[0];
        this.name = name;
        logger.debug("Loading model {} from file {}", name, file);
        loadModel(file);
        
        this.position = position;
        this.scale = scale;
        this.rotation = rotation;
        this.rotationAxis = rotationAxis.length() == 1f ? rotationAxis : new Vec3(1f, 0f, 0f);
        this.transform = transform;
    }
    
    private void loadModel(String file) throws IOException {
        AIScene scene = aiImportFile(DIRECTORY + file,
                aiProcess_Triangulate |/* aiProcess_FlipUVs |*/ aiProcess_OptimizeMeshes);
    
        if (scene == null || (scene.mFlags() & AI_SCENE_FLAGS_INCOMPLETE) != 0
                || scene.mRootNode() == null) {
            throw new IOException(aiGetErrorString());
        }
    
        logger.trace("{} materials found", scene.mNumMaterials());
        PointerBuffer mMaterials = scene.mMaterials();
        for (int i = 0; i < scene.mNumMaterials(); i++) {
            logger.trace("Processing material {}", i);
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
                transform.times(toMat4(parent.mTransformation()));
            }
            meshes.add(processMesh(AIMesh.create(mMeshes.get(node.mMeshes().get(i))), transform));
        }
        
        PointerBuffer mChildren = node.mChildren();
        if (mChildren == null) return;
        for (int i = 0; i < node.mNumChildren(); i++)
            processNode(AINode.create(mChildren.get()), scene);
    }
    
    private Mesh processMesh(AIMesh mesh, Mat4 transform) {
        Vector<Vec3> positions = new Vector<>();
        mesh.mVertices().forEach(vertex -> positions.add(new Vec3(vertex.x(), vertex.y(), vertex.z())));
        
        Vector<Vec3> normals = new Vector<>();
        if (mesh.mNormals() != null)
            mesh.mNormals().forEach(normal -> normals.add(new Vec3(normal.x(), normal.y(), normal.z())));
        
        Vector<Vec2> texCoords = new Vector<>();
        if (mesh.mTextureCoords(0) != null)
            mesh.mTextureCoords(0).forEach(texCoord -> texCoords.add(new Vec2(texCoord.x(), texCoord.y())));
        
        Vector<Vertex> vertices = new Vector<>();
        for (int i = 0; i < mesh.mNumVertices(); i++)
            vertices.add(new Vertex(
                    positions.get(i),
                    normals.size() > 0 ? normals.get(i) : new Vec3(),
                    texCoords.size() > 0 ?texCoords.get(i) : new Vec2())
            );
        
        Vector<Short> indices = new Vector<>();
        for (int i = 0; i < mesh.mNumFaces(); i++) {
            AIFace face = mesh.mFaces().get(i);
            IntBuffer buffer = face.mIndices();
            for (int j = 0; j < face.mNumIndices(); j++)
                indices.add((short) buffer.get());
        }
        
        logger.trace("Loaded mesh with {} vertices {} normals and {} uv coordinates", vertices.size(),
                normals.size() > 0 ? "with" : "without", texCoords.size() > 0 ? "with" : "without");
        
        Material material = materials.get(mesh.mMaterialIndex());
        return Mesh.Loader.loadToVAO(vertices, indices, material, transform);
    }
    
    private Material processMaterial(AIMaterial aiMaterial) {
        Material material = Material.getBlank();
        
        AIColor4D color = AIColor4D.create();
        aiGetMaterialColor(aiMaterial, "", 0, 0, color);
        
        for (int type : new int[] {
                aiTextureType_DIFFUSE, aiTextureType_SPECULAR, aiTextureType_EMISSIVE, aiTextureType_HEIGHT,
                aiTextureType_SHININESS //TODO use Texture.TextureType instead?
        }) addTexturesToMaterial(material, aiMaterial, type);
        
        logger.trace("Added {} total textures to material", material.getTextures().size());
        return material;
    }
    
    private void addTexturesToMaterial(Material material, AIMaterial aiMaterial, int textureType) {
        AIString file = AIString.create();
        int validTextures = 0;
    
        TextureType type = switch (textureType) {
            case aiTextureType_DIFFUSE -> TextureType.DIFFUSE;
            case aiTextureType_SPECULAR -> TextureType.SPECULAR;
            case aiTextureType_EMISSIVE -> TextureType.EMISSIVE;
            case aiTextureType_HEIGHT -> TextureType.HEIGHT;
            case aiTextureType_SHININESS -> TextureType.SHININESS;
            default -> TextureType.UNKNOWN;
        };
        
        for (int i = 0; i < aiGetMaterialTextureCount(aiMaterial, textureType); i++) {
            if (aiGetMaterialTexture(aiMaterial, textureType, i, file,
                    new int[1], null, null, null, null, null)
                    != aiReturn_SUCCESS) {
                System.err.println(aiGetErrorString());
                continue;
            }
            
            //TODO i think using the loader by default here is warranted, since textures are separate files, and
            // more often than not the bulk redundant data (probably), i should add an option to switch this off tho
            Texture texture = TextureLoader.get().load(
                    this.file + "_" + type.name().toLowerCase() + "_" + i,
                    () -> Texture.ofFile(file.dataString(), type));
            material.addTexture(texture);
            validTextures++;
        }
        
        logger.trace("{} {} textures loaded", validTextures, type.name().toLowerCase());
    }
    
    public String getName() {
        return name;
    }
    
    public Vector<Mesh> getMeshes() {
        return meshes;
    }
    
    public Vec3 getPosition() {
        return position;
    }
    
    public Model setPosition(Vec3 vec) {
        this.position.put(vec);
        return this;
    }
    
    public Vec3 getScale() {
        return scale;
    }
    
    public Model setScale(float scale) {
        this.scale.put(scale);
        return this;
    }
    
    public Model setScale(Vec3 scale) {
        this.scale.put(scale);
        return this;
    }
    
    public float getRotation() {
        return rotation;
    }
    
    public Vec3 getRotationAxis() {
        return rotationAxis;
    }
    
    public Model setRotation(float rotation, Vec3 rotationAxis) {
        this.rotation = rotation;
        this.rotationAxis.put(rotationAxis.normalize());
        return this;
    }
    
    //Transform is lazily updated
    public Mat4 getTransform() {
        updateTransform();
        return transform;
    }
    
    private void updateTransform() {
        this.transform.put(this.transform.identity()
                .translate(position)
                .scale(scale)
                .rotate(rotation, rotationAxis)
        );
    }
    
    private static Mat4 toMat4(AIMatrix4x4 mat) {
        float[] values = new float[] {
                mat.a1(), mat.a2(), mat.a3(), mat.a4(),
                mat.b1(), mat.b2(), mat.b4(), mat.b4(),
                mat.c1(), mat.c2(), mat.c3(), mat.c4(),
                mat.d1(), mat.d2(), mat.d3(), mat.d4()
        };
        return new Mat4(values).transpose();
    }
    
    @Override
    public void dispose() {
        meshes.forEach(Mesh::dispose);
        materials.forEach(Material::dispose);
    }
    
}
