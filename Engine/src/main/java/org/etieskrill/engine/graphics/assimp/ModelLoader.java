package org.etieskrill.engine.graphics.assimp;

import glm_.vec2.Vec2;
import glm_.vec2.Vec2i;
import glm_.vec3.Vec3;
import org.etieskrill.engine.entity.data.AABB;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.Textures;
import org.etieskrill.engine.util.Loaders;
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

public final class ModelLoader {

    public static final String DIRECTORY = "Engine/src/main/resources/models/";

    private static final Logger logger = LoggerFactory.getLogger(ModelLoader.class);

    static Model.Builder loadModel(Model.Builder builder) throws IOException {
        //TODO properly set these
        int processFlags =
                aiProcess_Triangulate |
                        (builder.flipUVs ? aiProcess_FlipUVs : 0) |
                        aiProcess_OptimizeMeshes |
                        aiProcess_OptimizeGraph |
                        aiProcess_JoinIdenticalVertices |
                        aiProcess_RemoveRedundantMaterials |
                        aiProcess_FindInvalidData |
                        aiProcess_GenUVCoords |
                        aiProcess_TransformUVCoords |
                        aiProcess_FindInstances |
                        aiProcess_PreTransformVertices |
                        (builder.flipWinding ? aiProcess_FlipWindingOrder : 0)
                ;

        AIScene scene = aiImportFile(DIRECTORY + builder.file, processFlags);

        if (scene == null || (scene.mFlags() & AI_SCENE_FLAGS_INCOMPLETE) != 0
                || scene.mRootNode() == null) {
            throw new IOException(aiGetErrorString());
        }

        Map<String, Texture2D.Builder> embeddedTextures = new HashMap<>();

        loadEmbeddedTextures(scene, embeddedTextures);
        loadMaterials(scene, builder, embeddedTextures);
        processNode(scene.mRootNode(), scene, builder);
        calculateModelBoundingBox(builder);

        aiReleaseImport(scene);

        logger.debug("Loaded model {} with {} mesh{} and {} material{}", builder.name,
                builder.meshes.size(), builder.meshes.size() == 1 ? "" : "es",
                builder.materials.size(), builder.materials.size() == 1 ? "" : "s");

        return builder;
    }

    private static void processNode(AINode node, AIScene scene, Model.Builder builder) {
        PointerBuffer mMeshes = scene.mMeshes();
        if (mMeshes == null) return;
        for (int i = 0; i < node.mNumMeshes(); i++)
            builder.meshes.add(processMesh(AIMesh.create(mMeshes.get(node.mMeshes().get(i))), builder));

        PointerBuffer mChildren = node.mChildren();
        if (mChildren == null) return;
        for (int i = 0; i < node.mNumChildren(); i++)
            processNode(AINode.create(mChildren.get()), scene, builder);
    }

    private static Mesh processMesh(AIMesh mesh, Model.Builder builder) {
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
                !normals.isEmpty() ? "with" : "without", !texCoords.isEmpty() ? "with" : "without");

        Material material = builder.materials.get(mesh.mMaterialIndex());
        return Mesh.Loader.loadToVAO(vertices, indices, material, boundingBox);
    }

    private static AABB calculateBoundingBox(List<Vertex> vertices) {
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

    private static void loadEmbeddedTextures(AIScene scene, Map<String, Texture2D.Builder> embeddedTextures) {
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

    private static void loadMaterials(AIScene scene, Model.Builder builder, Map<String, Texture2D.Builder> embeddedTextures) {
        logger.trace("{} materials found", scene.mNumMaterials());
        PointerBuffer mMaterials = scene.mMaterials();
        for (int i = 0; i < scene.mNumMaterials(); i++) {
            logger.trace("Processing material {}", i);
            builder.materials.add(processMaterial(AIMaterial.create(mMaterials.get()), builder, embeddedTextures));
        }
    }

    private static Material processMaterial(AIMaterial aiMaterial, Model.Builder builder, Map<String, Texture2D.Builder> embeddedTextures) {
        Material.Builder material = new Material.Builder();

        AIColor4D color = AIColor4D.create();
        aiGetMaterialColor(aiMaterial, "", 0, 0, color);

        for (AbstractTexture.Type type : new AbstractTexture.Type[] {
                DIFFUSE, SPECULAR, EMISSIVE, HEIGHT, SHININESS
        }) addTexturesToMaterial(material, aiMaterial, type, builder, embeddedTextures);

        int numProperties = addPropertiesToMaterial(material, aiMaterial);

        Material mat = material.build();
        logger.trace("Added {} texture{} and {} propert{} to material", mat.getTextures().size(),
                mat.getTextures().size() == 1 ? "" : "s", numProperties, numProperties == 1 ? "y" : "ies");
        return mat;
    }

    private static void addTexturesToMaterial(Material.Builder material, AIMaterial aiMaterial, AbstractTexture.Type type, Model.Builder builder, Map<String, Texture2D.Builder> embeddedTextures) {
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
            String textureName = builder.name + "_" + type.name().toLowerCase() + "_" + i;
            String textureFile = file.dataString();

            Texture2D.Builder textureBuilder = embeddedTextures.get(textureFile);
            Supplier<AbstractTexture> supplier;
            if (textureBuilder != null)
                supplier = () -> textureBuilder.setType(type).build();
            else
                supplier = () -> Textures.ofFile(textureFile, type);

            Texture2D texture = (Texture2D) Loaders.TextureLoader.get().load(textureName, supplier);
            material.addTextures(texture);
            validTextures++;
        }

        logger.trace("{} {} textures loaded", validTextures, type.name().toLowerCase());
    }

    private static int addPropertiesToMaterial(Material.Builder material, AIMaterial aiMaterial) {
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

    private static void calculateModelBoundingBox(Model.Builder builder) {
        Vec3 min = new Vec3(), max = new Vec3();
        for (Mesh mesh : builder.meshes) {
            Vec3 meshMin = mesh.getBoundingBox().getMin();
            Vec3 meshMax = mesh.getBoundingBox().getMax();
            min.put(Math.min(meshMin.getX(), min.getX()), Math.min(meshMin.getY(), min.getY()), Math.min(meshMin.getZ(), min.getZ()));
            max.put(Math.max(meshMax.getX(), max.getX()), Math.max(meshMax.getY(), max.getY()), Math.max(meshMax.getZ(), max.getZ()));
        }
        builder.boundingBox = new AABB(min, max);
    }

}
