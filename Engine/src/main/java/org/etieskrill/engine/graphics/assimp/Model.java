package org.etieskrill.engine.graphics.assimp;

import glm_.mat4x4.Mat4;
import glm_.vec2.Vec2;
import glm_.vec2.Vec2i;
import glm_.vec3.Vec3;
import glm_.vec4.Vec4;
import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.entity.data.AABB;
import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.AbstractTexture.Type;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.Textures;
import org.etieskrill.engine.util.Loaders.TextureLoader;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.function.Supplier;

import static org.etieskrill.engine.graphics.texture.AbstractTexture.Type.*;
import static org.lwjgl.assimp.Assimp.*;

//TODO refactor: loading in separate class / classes
//               reduce to data in anticipation of ces
//               find most comprehensive solution for multi-entry-point builders
public class Model implements Disposable {
    
    private static final String DIRECTORY = "Engine/src/main/resources/models/";
    private static final Supplier<Model> ERROR_MODEL = () -> new Builder("cube.obj").build();
    
    private static final Logger logger = LoggerFactory.getLogger(Model.class);
    
    private final List<Mesh> meshes; //TODO these should become immutable after model instantiation
    private final List<Material> materials; //TODO since meshes know their materials, these here may not be necessary?
    
    //TODO this really does not belong here, loading should FINALLY be transferred to a loader...
    // throw together with the ModelLoader? nah, better write a separate one, though that is a bit disgusting
    private final Map<String, Texture2D.Builder> embeddedTextures = new HashMap<>();
    
    private AABB boundingBox;
    
    private final String name;
    
    private final Transform transform;
    
    private boolean flipUVs;
    private boolean flipWinding;
    private final boolean culling;
    private final boolean transparency;
    
    //TODO move to entity eventually
    private boolean enabled;
    
    public static class Builder {
        protected final List<Mesh> meshes = new LinkedList<>();
        protected final List<Material> materials = new LinkedList<>();
    
        public AABB boundingBox;
        
        protected final String file;
        protected String name;
    
        protected boolean flipUVs = true;
        protected boolean flipWinding = false;
        protected boolean culling = true;
        protected boolean transparency = false;
        
        protected Transform transform = Transform.getBlank();
        
        public Builder(String file) {
            if (file.isBlank()) throw new IllegalArgumentException("File name cannot be blank");
            if (file.contains("/")) throw new IllegalArgumentException("Custom folder structure not implemented yet: " + file);
            
            this.file = file;
            this.name = file.split("\\.")[0];
        }
    
        //TODO actually integrate this & material setter into loading process
        public Builder setMeshes(Mesh... meshes) {
            this.meshes.clear();
            this.meshes.addAll(List.of(meshes));
            return this;
        }
    
        public Builder setMaterials(Material... materials) {
            this.materials.clear();
            this.materials.addAll(List.of(materials));
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
        
        public Builder setFlipUVs(boolean flipUVs) {
            this.flipUVs = flipUVs;
            return this;
        }
    
        public Builder setFlipWinding(boolean flipWinding) {
            this.flipWinding = flipWinding;
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
        
        public Builder setTransform(Transform transform) {
            this.transform = transform;
            return this;
        }
    
        public Model build() {
            try {
                return new Model(this);
            } catch (IOException e) {
                logger.info("Exception while loading model, using default: ", e);
                return ERROR_MODEL.get();
            }
        }
    }
    
    //TODO this is a very very very Very VERY temporary solution, i can hardly look at it
    public static class MemoryBuilder extends Builder {
        public MemoryBuilder(String name) {
            super(name);
        }
    
        @Override
        public Model build() {
            return new Model(this);
        }
    }
    
    public static Model ofFile(String file) {
        return ofFile(file, true);
    }
    
    public static Model ofFile(String file, boolean flipUVs) {
        return new Builder(file).setFlipUVs(flipUVs).build();
    }
    
    /**
     * Copy constructor, for ... not cloning.
     */
    public Model(Model model) {
        //TODO since the below three lines represent the model as loaded into the graphics memory
        // and should effectively be immutable, consider encapsulating them into another class
        this.meshes = model.meshes;
        this.materials = model.materials;
        this.boundingBox = model.boundingBox;
        this.name = model.name;
        
        logger.trace("Creating copy of model {}", name);
        
        this.transform = new Transform(model.transform);
        this.flipUVs = model.flipUVs;
        this.flipWinding = model.flipWinding;
        this.culling = model.culling;
        this.transparency = model.transparency;
        
        this.enabled = model.enabled;
    }
    
    private Model(Builder builder) throws IOException {
        this.meshes = builder.meshes;
        this.materials = builder.materials;
        
        this.name = builder.name;
    
        this.flipUVs = builder.flipUVs;
        this.flipWinding = builder.flipWinding;
        this.culling = builder.culling;
        this.transparency = builder.transparency;
    
        logger.debug("Loading model {} from file {}", name, builder.file);
        loadModel(builder.file);
        
        this.transform = builder.transform;
        
        enable();
    }
    
    private Model(MemoryBuilder builder) {
        this.meshes = builder.meshes;
        this.materials = builder.materials;
        
        this.name = builder.name;
        
        this.flipUVs = builder.flipUVs;
        this.flipWinding = builder.flipWinding;
        this.culling = builder.culling;
        this.transparency = builder.transparency;
        
        logger.debug("Loading model {} from memory", name);
        
        this.transform = builder.transform;
        
        enable();
    }
    
    private void loadModel(String file) throws IOException {
        //TODO properly set these
        int processFlags =
            aiProcess_Triangulate |
            (flipUVs ? aiProcess_FlipUVs : 0) |
            aiProcess_OptimizeMeshes |
            aiProcess_JoinIdenticalVertices |
            aiProcess_RemoveRedundantMaterials |
            aiProcess_FindInvalidData |
            aiProcess_GenUVCoords |
            aiProcess_TransformUVCoords |
            aiProcess_FindInstances |
            aiProcess_PreTransformVertices |
            (flipWinding ? aiProcess_FlipWindingOrder : 0)
        ;
        
        AIScene scene = aiImportFile(DIRECTORY + file, processFlags);
    
        if (scene == null || (scene.mFlags() & AI_SCENE_FLAGS_INCOMPLETE) != 0
                || scene.mRootNode() == null) {
            throw new IOException(aiGetErrorString());
        }
        
        loadEmbeddedTextures(scene);
        loadMaterials(scene);
        processNode(scene.mRootNode(), scene);
        calculateModelBoundingBox();
        
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
        //TODO add AABB loading but oh FUCK each goddamn mesh has a separate one FFS
        
        int numVertices = mesh.mNumVertices();
        List<Vec3> positions = new ArrayList<>(numVertices);
        mesh.mVertices().forEach(vertex -> positions.add(new Vec3(vertex.x(), vertex.y(), vertex.z())));

        List<Vec3> normals = new ArrayList<>(numVertices);
        if (mesh.mNormals() != null)
            mesh.mNormals().forEach(normal -> normals.add(new Vec3(normal.x(), normal.y(), normal.z())));

        List<Vec2> texCoords = new ArrayList<>(numVertices);
        if (mesh.mTextureCoords(0) != null)
            mesh.mTextureCoords(0).forEach(texCoord -> texCoords.add(new Vec2(texCoord.x(), texCoord.y())));

        List<Vertex> vertices = new ArrayList<>(numVertices);
        for (int i = 0; i < mesh.mNumVertices(); i++)
            vertices.add(new Vertex(
                    positions.get(i),
                    normals.size() > 0 ? normals.get(i) : new Vec3(),
                    texCoords.size() > 0 ?texCoords.get(i) : new Vec2())
            );

        //three because a face is usually a triangle, but this list is discarded at the first opportunity a/w
        List<Short> indices = new ArrayList<>(mesh.mNumFaces() * 3);
        for (int i = 0; i < mesh.mNumFaces(); i++) {
            AIFace face = mesh.mFaces().get(i);
            IntBuffer buffer = face.mIndices();
            for (int j = 0; j < face.mNumIndices(); j++)
                indices.add((short) buffer.get());
        }
        
        AIVector3D min = mesh.mAABB().mMin();
        AIVector3D max = mesh.mAABB().mMax();
        AABB boundingBox = new AABB(new Vec3(min.x(), min.y(), min.z()),
                                    new Vec3(max.x(), max.y(), max.z()));
        
        if (boundingBox.getMin().allEqual(0f, 0.00001f)
                || boundingBox.getMax().allEqual(0f, 0.00001f))
            boundingBox = calculateBoundingBox(vertices);
        
        logger.trace("Loaded mesh with {} vertices {} normals, {} uv coordinates", vertices.size(),
                normals.size() > 0 ? "with" : "without", texCoords.size() > 0 ? "with" : "without");
        
        Material material = materials.get(mesh.mMaterialIndex());
        return Mesh.Loader.loadToVAO(vertices, indices, material, boundingBox);
    }
    
    private AABB calculateBoundingBox(List<Vertex> vertices) {
        float minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;
        
        //for models as complicated as the skeleton, a stream variant presents a slight improvement in performance,
        //measurable in the single milliseconds, which is not worth the cost of initialising a stream for a model with
        //very few vertices
        for (Vertex vertex : vertices) {
            Vec3 pos = vertex.getPosition();
            minX = Math.min(pos.getX(), minX);
            minY = Math.min(pos.getY(), minY);
            minZ = Math.min(pos.getZ(), minZ);
            maxX = Math.max(pos.getX(), maxX);
            maxY = Math.max(pos.getY(), maxY);
            maxZ = Math.max(pos.getZ(), maxZ);
        }
        
        return new AABB(new Vec3(minX, minY, minZ), new Vec3(maxX, maxY, maxZ));
    }
    
    private void loadEmbeddedTextures(AIScene scene) {
        PointerBuffer textures = scene.mTextures();
        for (int i = 0; i < scene.mNumTextures(); i++) {
            AITexture texture = AITexture.create(textures.get());
            
            //TODO data should really always be compressed when embedded, but at least add a warning or something in case it is not
            // also "texture data is always ARGB8888 to make the implementation for user of the library as easy as possible" my arse, thats just inefficient
//            if (texture.mHeight() != 0) is uncompressed;
//            else is compressed;
            
            ByteBuffer compressedBuffer = texture.pcDataCompressed();
            byte[] compressedData = new byte[compressedBuffer.remaining()];
            compressedBuffer.get(compressedData);
            
            BufferedImage image;
            try {
                image = ImageIO.read(new ByteArrayInputStream(compressedData)); //TODO dunno if i like depending on plugins, but isss brobably fiiine
            } catch (IOException e) {
                logger.warn("Failed to decode embedded texture {}:\n{}", i, e.getMessage());
                continue;
            }
            
            //TODO validate colour channels if not compressed
//            System.out.println(image.getType());
            
            int[] data = image.getData().getPixels(0, 0, image.getWidth(), image.getHeight(), (int[]) null);
            ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
            for (int pixel : data) buffer.put((byte) pixel);
            buffer.rewind();
            
            Texture2D.Builder tex = new Texture2D.BufferBuilder(buffer,
                    new Vec2i(image.getWidth(), image.getHeight()), AbstractTexture.Format.RGBA);
            embeddedTextures.put("*" + i, tex);
        }
        
        logger.debug("{} of {} embedded textures loaded", embeddedTextures.size(), scene.mNumTextures());
    }
    
    private void loadMaterials(AIScene scene) {
        logger.trace("{} materials found", scene.mNumMaterials());
        PointerBuffer mMaterials = scene.mMaterials();
        for (int i = 0; i < scene.mNumMaterials(); i++) {
            logger.trace("Processing material {}", i);
            materials.add(processMaterial(AIMaterial.create(mMaterials.get())));
        }
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
        logger.trace("Added {} texture{} and {} propert{} to material", mat.getTextures().size(),
                mat.getTextures().size() == 1 ? "" : "s", numProperties, numProperties == 1 ? "y" : "ies");
        return mat;
    }
    
    private void addTexturesToMaterial(Material.Builder material, AIMaterial aiMaterial, Type type) {
        AIString file = AIString.create();
    
        int aiTextureType = type.ai();
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
            String textureName = this.name + "_" + type.name().toLowerCase() + "_" + i;
            String textureFile = file.dataString();
            
            Texture2D.Builder builder = embeddedTextures.get(textureFile);
            Supplier<AbstractTexture> supplier;
            if (builder != null)
                supplier = () -> builder.setType(type).build();
            else
                supplier = () -> Textures.ofFile(textureFile, type);
            
            Texture2D texture = (Texture2D) TextureLoader.get().load(textureName, supplier);
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
    
    private void calculateModelBoundingBox() {
        Vec3 min = new Vec3(), max = new Vec3();
        for (Mesh mesh : meshes) {
            Vec3 meshMin = mesh.getBoundingBox().getMin();
            Vec3 meshMax = mesh.getBoundingBox().getMax();
            min.put(Math.min(meshMin.getX(), min.getX()), Math.min(meshMin.getY(), min.getY()), Math.min(meshMin.getZ(), min.getZ()));
            max.put(Math.max(meshMax.getX(), max.getX()), Math.max(meshMax.getY(), max.getY()), Math.max(meshMax.getZ(), max.getZ()));
        }
        this.boundingBox = new AABB(min, max);
    }
    
    public AABB getBoundingBox() {
        return boundingBox;
    }
    
    public AABB getWorldBoundingBox() {
        Mat4 transform = this.transform.toMat();
        return new AABB(transform.times(new Vec4(boundingBox.getMin())).toVec3(),
                        transform.times(new Vec4(boundingBox.getMax())).toVec3());
    }
    
    public String getName() {
        return name;
    }
    
    public List<Mesh> getMeshes() {
        return meshes;
    }
    
    public Transform getTransform() {
        return transform;
    }
    
    public boolean doCulling() {
        return culling;
    }
    
    public boolean hasTransparency() {
        return transparency;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void enable() {
        enabled = true;
    }
    
    public void disable() {
        enabled = false;
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
