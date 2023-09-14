package org.etieskrill.engine.graphics.assimp;

import glm_.mat4x4.Mat4;
import glm_.vec2.Vec2;
import glm_.vec3.Vec3;
import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.graphics.gl.Loaders.TextureLoader;
import org.etieskrill.engine.graphics.gl.Texture;
import org.etieskrill.engine.graphics.gl.Texture.Type;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Vector;
import java.util.function.Supplier;

import static org.etieskrill.engine.graphics.gl.Texture.Type.*;
import static org.lwjgl.assimp.Assimp.*;

public class Model implements Disposable {
    
    private static final String DIRECTORY = "Engine/src/main/resources/models/";
    private static final Supplier<Model> ERROR_MODEL = () -> new Builder("cube.obj").build();
    
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
    
    private final boolean culling;
    private final boolean transparency;
    
    public static class Builder {
        private final Vector<Mesh> meshes = new Vector<>();
        private final Vector<Material> materials = new Vector<>();
    
        private final String file;
        private String name;
    
        private boolean culling = true;
        private boolean transparency = false;
        
        public Builder(String file) {
            if (file.isBlank()) throw new IllegalArgumentException("File name cannot be blank");
            if (file.contains("/")) throw new IllegalArgumentException("Custom folder structure not implemented yet: " + file);
            
            this.file = file;
            this.name = file.split("\\.")[0];
        }
    
        public Builder setMeshes(Mesh... meshes) {
            this.meshes.clear();
            this.meshes.addAll(new Vector<>(List.of(meshes)));
            return this;
        }
    
        public Builder setMaterials(Material... materials) {
            this.materials.clear();
            this.materials.addAll(new Vector<>(List.of(materials)));
            return this;
        }
        
        public Builder removeMaterials() {
            this.materials.clear();
            return this;
        }
    
        public Builder setName(String name) {
            this.name = name;
            return this;
        }
    
        public Builder disableCulling() {
            this.culling = false;
            return this;
        }
    
        public Builder hasTransparency() {
            transparency = true;
            return this;
        }
        
        //TODO add refractive toggle/mode (NONE, GLASS, WATER etc.)
    
        public Model build() {
            try {
                return new Model(file, name, meshes, materials,
                        new Vec3(0f), new Vec3(1f), 0f, new Vec3(0f), new Mat4(),
                        culling, transparency);
            } catch (IOException e) {
                logger.info("Exception while loading model, using default: ", e);
                return ERROR_MODEL.get();
            }
        }
    }
    
    public static Model ofFile(String file) {
        return new Builder(file).build();
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
        this.culling = model.culling;
        this.transparency = model.transparency;
    }
    
    //TODO contemplate whether to pass on builders in cases like these
    private Model(String file, String name, Vector<Mesh> meshes, Vector<Material> materials,
                 Vec3 position, Vec3 scale, float rotation, Vec3 rotationAxis, Mat4 transform,
                  boolean culling, boolean transparency) throws IOException {
        
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
        
        this.culling = culling;
        this.transparency = transparency;
    }
    
    private void loadModel(String file) throws IOException {
        //TODO properly set these
        int processFlags =
            aiProcess_Triangulate |
            aiProcess_FlipUVs |
            aiProcess_OptimizeMeshes |
            aiProcess_JoinIdenticalVertices |
            aiProcess_RemoveRedundantMaterials |
            aiProcess_FindInvalidData |
            aiProcess_GenUVCoords |
            aiProcess_TransformUVCoords |
            aiProcess_FindInstances |
            aiProcess_PreTransformVertices //|
            //aiProcess_FlipWindingOrder
        ;
        
        AIScene scene = aiImportFile(DIRECTORY + file, processFlags);
    
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
        
        aiReleaseImport(scene);
    }
    
    private void processNode(AINode node, AIScene scene) {
        PointerBuffer mMeshes = scene.mMeshes();
        if (mMeshes == null) return;
        for (int i = 0; i < node.mNumMeshes(); i++)
            meshes.add(processMesh(AIMesh.create(mMeshes.get(node.mMeshes().get(i)))));
        
        PointerBuffer mChildren = node.mChildren();
        if (mChildren == null) return;
        for (int i = 0; i < node.mNumChildren(); i++)
            processNode(AINode.create(mChildren.get()), scene);
    }
    
    private Mesh processMesh(AIMesh mesh) {
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
        return Mesh.Loader.loadToVAO(vertices, indices, material);
    }
    
    private Material processMaterial(AIMaterial aiMaterial) {
        Material.Builder material = new Material.Builder();
        
        AIColor4D color = AIColor4D.create();
        aiGetMaterialColor(aiMaterial, "", 0, 0, color);
        
        for (Type type : new Type[] {
                DIFFUSE, SPECULAR, EMISSIVE, HEIGHT, SHININESS
        }) addTexturesToMaterial(material, aiMaterial, type);
        
        int numProperties = addPropertiesToMaterial(material, aiMaterial);
        
        Material mat = material.build();
        logger.trace("Added {} textures and {} properties to material", mat.getTextures().size(), numProperties);
        return mat;
    }
    
    private void addTexturesToMaterial(Material.Builder material, AIMaterial aiMaterial, Type type) {
        AIString file = AIString.create();
    
        int aiTextureType = type.toAI();
        int validTextures = 0;
        for (int i = 0; i < aiGetMaterialTextureCount(aiMaterial, aiTextureType); i++) {
            if (aiGetMaterialTexture(aiMaterial, aiTextureType, i, file,
                    new int[1], null, null, null, null, null)
                    != aiReturn_SUCCESS) {
                logger.warn("Error while loading material texture: {}", aiGetErrorString());
                continue;
            }
            
            //TODO i think using the loader by default here is warranted, since textures are separate files, and
            // more often than not the bulk redundant data (probably), i should add an option to switch this off tho
            Texture texture = TextureLoader.get().load(
                    this.file + "_" + type.name().toLowerCase() + "_" + i,
                    () -> Texture.ofFile(file.dataString(), type));
            material.addTextures(texture);
            validTextures++;
        }
        
        logger.trace("{} {} textures loaded", validTextures, type.name().toLowerCase());
    }
    
    private int addPropertiesToMaterial(Material.Builder material, AIMaterial aiMaterial) {
        int validProperties = 0;
        PointerBuffer propBuffer = BufferUtils.createPointerBuffer(1);
        
        if (aiReturn_SUCCESS == aiGetMaterialProperty(aiMaterial, AI_MATKEY_SHININESS, propBuffer)) {
            logger.trace("Material shininess property found");
            ByteBuffer buffer = AIMaterialProperty.create(propBuffer.get()).mData();
            material.setShininess(buffer.getFloat());
            validProperties++;
        }
        
        if (aiReturn_SUCCESS == aiGetMaterialProperty(aiMaterial, AI_MATKEY_SHININESS_STRENGTH, propBuffer.clear())) {
            logger.trace("Material shininess strength property found");
            ByteBuffer buffer = AIMaterialProperty.create(propBuffer.get()).mData();
            material.setShininessStrength(buffer.getFloat());
            validProperties++;
        }
        
        return validProperties;
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
    
    public boolean doCulling() {
        return culling;
    }
    
    public boolean hasTransparency() {
        return transparency;
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
