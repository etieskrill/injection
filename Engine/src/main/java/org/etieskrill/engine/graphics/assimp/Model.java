package org.etieskrill.engine.graphics.assimp;

import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.entity.data.AABB;
import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.AbstractTexture.Type;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.Textures;
import org.etieskrill.engine.util.Loaders.TextureLoader;
import org.etieskrill.engine.util.ResourceReader;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.function.Supplier;

import static org.etieskrill.engine.config.ResourcePaths.MODEL_PATH;
import static org.etieskrill.engine.graphics.assimp.Material.Property.SHININESS;
import static org.etieskrill.engine.graphics.assimp.Material.Property.*;
import static org.etieskrill.engine.graphics.texture.AbstractTexture.Type.*;
import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memCopy;

//TODO refactor: loading in separate class / classes
//               reduce to data in anticipation of ces
//               find most comprehensive solution for multi-entry-point builders
public class Model implements Disposable {
    
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
                    aiProcess_SortByPType |
            (flipUVs ? aiProcess_FlipUVs : 0) |
            aiProcess_OptimizeMeshes |
            aiProcess_OptimizeGraph |
            aiProcess_JoinIdenticalVertices |
            aiProcess_RemoveRedundantMaterials |
            aiProcess_FindInvalidData |
            aiProcess_GenUVCoords |
            aiProcess_TransformUVCoords |
            aiProcess_FindInstances |
            aiProcess_PreTransformVertices |
            (flipWinding ? aiProcess_FlipWindingOrder : 0)
        ;

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
        AIScene scene = aiImportFileEx(file, processFlags, fileIO);
        if (scene == null || (scene.mFlags() & AI_SCENE_FLAGS_INCOMPLETE) != 0
                || scene.mRootNode() == null) {
            throw new IOException(aiGetErrorString());
        }
        
        loadEmbeddedTextures(scene);
        loadMaterials(scene);
        processNode(scene.mRootNode(), scene);
        calculateModelBoundingBox();

        aiReleaseImport(scene);
        
        logger.debug("Loaded model {} with {} mesh{} and {} material{}", name,
                meshes.size(), meshes.size() == 1 ? "" : "es",
                materials.size(), materials.size() == 1 ? "" : "s");
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
        List<Vector3fc> positions = new ArrayList<>(numVertices);
        mesh.mVertices().forEach(vertex -> positions.add(new Vector3f(vertex.x(), vertex.y(), vertex.z())));

        List<Vector3fc> normals = new ArrayList<>(numVertices);
        if (mesh.mNormals() != null)
            mesh.mNormals().forEach(normal -> normals.add(new Vector3f(normal.x(), normal.y(), normal.z())));

        List<Vector2fc> texCoords = new ArrayList<>(numVertices);
        if (mesh.mTextureCoords(0) != null)
            mesh.mTextureCoords(0).forEach(texCoord -> texCoords.add(new Vector2f(texCoord.x(), texCoord.y())));

        List<Vertex> vertices = new ArrayList<>(numVertices);
        for (int i = 0; i < mesh.mNumVertices(); i++)
            vertices.add(new Vertex(
                    positions.get(i),
                    normals.size() > 0 ? normals.get(i) : new Vector3f(),
                    texCoords.size() > 0 ? texCoords.get(i) : new Vector2f())
            );

        //three because a face is usually a triangle, but this list is discarded at the first opportunity a/w
        //TODO add loader versions which transmit the minimal amount of data (shorts for indices, smaller vectors)
        List<Integer> indices = new ArrayList<>(mesh.mNumFaces() * 3);
        for (int i = 0; i < mesh.mNumFaces(); i++) {
            AIFace face = mesh.mFaces().get(i);
            IntBuffer buffer = face.mIndices();
            for (int j = 0; j < face.mNumIndices(); j++)
                indices.add(buffer.get());
        }

        List<Bone> bones = getBones(mesh);
        
        AIVector3D min = mesh.mAABB().mMin();
        AIVector3D max = mesh.mAABB().mMax();
        AABB boundingBox = new AABB(new Vector3f(min.x(), min.y(), min.z()),
                new Vector3f(max.x(), max.y(), max.z()));

        if (boundingBox.getMin().equals(new Vector3f(), 0.00001f)
                || boundingBox.getMax().equals(new Vector3f(), 0.00001f))
            boundingBox = calculateBoundingBox(vertices);

        logger.trace("Loaded mesh with {} vertices and {} indices, {} normals, {} uv coordinates", vertices.size(),
                indices.size(), !normals.isEmpty() ? "with" : "without", !texCoords.isEmpty() ? "with" : "without");
        
        Material material = materials.get(mesh.mMaterialIndex());
        return Mesh.Loader.loadToVAO(vertices, indices, material, boundingBox);
    }

    private static class Bone {
    }

    private List<Bone> getBones(AIMesh mesh) {
        List<Bone> bones = new ArrayList<>(mesh.mNumBones());
        for (int i = 0; i < mesh.mNumBones(); i++) {
            AIBone aiBone = AIBone.create(mesh.mBones().get());
            String name = aiBone.mName().dataString();

//            aiBone.
//            bones.add();
        }
        return bones;
    }

    private static Matrix4f fromAI(AIMatrix4x4 mat) {
        return new Matrix4f(
                mat.a1(), mat.b1(), mat.c1(), mat.d1(),
                mat.a2(), mat.b2(), mat.c2(), mat.d2(),
                mat.a3(), mat.b3(), mat.c3(), mat.d3(),
                mat.a4(), mat.b4(), mat.c4(), mat.d4()
        );
    }
    
    private AABB calculateBoundingBox(List<Vertex> vertices) {
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
                    new Vector2i(image.getWidth(), image.getHeight()), AbstractTexture.Format.SRGBA);
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
        
        for (Type type : new Type[] {
                DIFFUSE, SPECULAR, EMISSIVE, HEIGHT, Type.SHININESS
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

        //String properties
        AIString matName = AIString.create();
        if (aiReturn_SUCCESS == aiGetMaterialString(aiMaterial, AI_MATKEY_NAME, aiTextureType_NONE, 0, matName)) {
            material.setProperty(Material.Property.NAME, matName.dataString());
            validProperties++;
        }

        //Vector4f properties
        AIColor4D colour = AIColor4D.calloc();
        for (Material.Property property : new Material.Property[]{
                COLOUR_BASE, COLOUR_AMBIENT, COLOUR_DIFFUSE, COLOUR_SPECULAR, COLOUR_EMISSIVE
        }) {
            if (aiReturn_SUCCESS == aiGetMaterialColor(aiMaterial, property.ai(), aiTextureType_NONE, 0, colour)) {
                Vector4fc colourProperty = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
                material.setProperty(property, colourProperty);
                validProperties++;
            }
        }

        //Single value properties
        PointerBuffer propBuffer = BufferUtils.createPointerBuffer(1);
        for (Material.Property property : new Material.Property[]{
                INTENSITY_EMISSIVE, SHININESS, SHININESS_STRENGTH, METALLIC_FACTOR, OPACITY
        }) {
            if (aiReturn_SUCCESS == aiGetMaterialProperty(aiMaterial, property.ai(), aiTextureType_NONE, 0, propBuffer)) {
                material.setProperty(property, AIMaterialProperty.create(propBuffer.get()).mData().getFloat());
                propBuffer.rewind();
                validProperties++;
            }
        }

        return validProperties;
    }
    
    private void calculateModelBoundingBox() {
        Vector3f min = new Vector3f(), max = new Vector3f();
        for (Mesh mesh : meshes) {
            mesh.getBoundingBox().getMin().min(min, min);
            mesh.getBoundingBox().getMax().max(max, max);
        }
        this.boundingBox = new AABB(min, max);
    }
    
    public AABB getBoundingBox() {
        return boundingBox;
    }
    
    public AABB getWorldBoundingBox() {
        Matrix4f worldTransform = this.transform.toMat();
        return new AABB(worldTransform.transformPosition(new Vector3f(boundingBox.getMin())),
                worldTransform.transformPosition(new Vector3f(boundingBox.getMax())));
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

    private static Matrix4f toMatrix4f(AIMatrix4x4 mat) {
        return new Matrix4f(
                mat.a1(), mat.a2(), mat.a3(), mat.a4(),
                mat.b1(), mat.b2(), mat.b4(), mat.b4(),
                mat.c1(), mat.c2(), mat.c3(), mat.c4(),
                mat.d1(), mat.d2(), mat.d3(), mat.d4()
        ).transpose();
    }
    
    @Override
    public void dispose() {
        meshes.forEach(Mesh::dispose);
        materials.forEach(Material::dispose);
    }
    
}
