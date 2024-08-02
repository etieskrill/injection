package org.etieskrill.engine.graphics.model.loader;

import org.etieskrill.engine.common.ResourceLoadException;
import org.etieskrill.engine.graphics.model.Material;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.AbstractTexture.Format;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.Textures;
import org.etieskrill.engine.util.FileUtils;
import org.etieskrill.engine.util.FileUtils.TypedFile;
import org.etieskrill.engine.util.Loaders;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.joml.Vector4fc;
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
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

import static org.etieskrill.engine.graphics.model.Material.Property.SHININESS;
import static org.etieskrill.engine.graphics.model.Material.Property.*;
import static org.etieskrill.engine.graphics.texture.AbstractTexture.Type.*;
import static org.etieskrill.engine.util.FileUtils.splitTypeFromPath;
import static org.lwjgl.assimp.Assimp.*;

class MaterialLoader {

    private static final Logger logger = LoggerFactory.getLogger(MaterialLoader.class);

    static void loadEmbeddedTextures(AIScene scene, Map<String, Texture2D.Builder> embeddedTextures) {
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

            Format format = switch (image.getType()) {
                case 5 -> Format.RGB; //TODO these are not the S* variants, i think, but this needs some more pondering
                case 6 -> Format.RGBA;
                case 10 -> Format.GRAY;
                //if this fails, refer to java.awt.image.BufferedImage#getType(), the formats are a bit cryptic, and i'd rather throw an exception than try to guess formats
                default -> throw new ResourceLoadException("Unknown texture format: '" + image.getType() + "'");
            };

            String filePath = texture.mFilename().dataString();

            int[] data = image.getData().getPixels(0, 0, image.getWidth(), image.getHeight(), (int[]) null);
            ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
            for (int pixel : data) buffer.put((byte) pixel);
            buffer.rewind();

            Texture2D.Builder tex = new Texture2D.BufferBuilder(buffer,
                    new Vector2i(image.getWidth(), image.getHeight()), format);
            tex.setType(determineType(filePath)); //take a guess at the type, since there is no way i see to get the type otherwise. the type is overridden when an embedded texture is loaded anyway
            embeddedTextures.put("*" + i, tex);
            embeddedTextures.put(filePath, tex); //materials usually reference the standard form of embedded textures (*0, *1 etc.) but some formats sometimes just use the path
            //TODO it's probably more correct to translate paths - if they exist - to the embedded format using a separate map, but this should do fine for now
            logger.trace("Loaded embedded texture no. {} {}: {}", i, filePath, tex);
        }

        logger.debug("{} of {} embedded textures loaded", embeddedTextures.size() / 2, scene.mNumTextures());
    }

    private static AbstractTexture.Type determineType(String filePath) {
        String[] splitFilePath = filePath.split("/");
        String fileName = splitFilePath[splitFilePath.length - 1];

        AbstractTexture.Type type = UNKNOWN;
        if (fileName.contains("diffuse")) type = DIFFUSE;
        else if (fileName.contains("specular")) type = SPECULAR;
        else if (fileName.contains("emissive") || fileName.contains("emission")) type = EMISSIVE;
        else if (fileName.contains("normal")) type = NORMAL;
        return type;
    }

    static void loadMaterials(AIScene scene, Model.Builder builder) {
        logger.debug("{} materials found", scene.mNumMaterials());
        PointerBuffer mMaterials = scene.mMaterials();
        for (int i = 0; i < scene.mNumMaterials(); i++) {
            logger.trace("Processing material {}", i);
            builder.getMaterials().add(
                    processMaterial(i, AIMaterial.create(
                                    mMaterials.get()),
                            builder.getEmbeddedTextures(),
                            builder.getName())
            );
        }
    }

    private static Material processMaterial(
            int materialIndex,
            AIMaterial aiMaterial,
            Map<String, Texture2D.Builder> embeddedTextures,
            String modelName
    ) {
        Material.Builder material = new Material.Builder();

        for (AbstractTexture.Type type : new AbstractTexture.Type[]{
                DIFFUSE, SPECULAR, EMISSIVE, HEIGHT, AbstractTexture.Type.SHININESS, NORMAL, METALNESS, DIFFUSE_ROUGHNESS
        }) {
            addTexturesToMaterial(materialIndex, material, aiMaterial, type, embeddedTextures, modelName);
        }

        addPropertiesToMaterial(material, aiMaterial);

        Material mat = material.build();
        int numProperties = material.getProperties().size();
        logger.debug("Added {} texture{} and {} propert{} to material", mat.getTextures().size(),
                mat.getTextures().size() == 1 ? "" : "s", numProperties, numProperties == 1 ? "y" : "ies");
        return mat;
    }

    private static void addTexturesToMaterial(
            int materialIndex,
            Material.Builder material,
            AIMaterial aiMaterial,
            AbstractTexture.Type type,
            Map<String, Texture2D.Builder> embeddedTextures,
            String modelName
    ) {
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
            String textureName = modelName + "_mat" + materialIndex + "_" + type.name().toLowerCase() + "_" + i;
            TypedFile textureFile = splitTypeFromPath(file.dataString());

            Supplier<AbstractTexture> supplier;
            if (embeddedTextures.containsKey(textureFile.getFullPath())) {
                logger.trace("Texture '{}' is loaded from embedded textures", textureName);
                supplier = () -> {
                    var textureBuilder = embeddedTextures.get(textureFile.getFullPath());
                    return textureBuilder
                            //textures having special formats (GL_RGB_{F16,I8,5} etc.) are not carried over by the below
                            //call, which is probably fine for embedded textures
                            .setFormat(Format.fromChannelsAndType(textureBuilder.getFormat().getChannels(), type))
                            .setType(type)
                            .build();
                };
            } else if (Textures.exists(textureFile.getFullPath())) {
                logger.trace("Texture '{}' is loaded from file {}", textureName, textureFile);
                supplier = () -> Textures.ofFile(textureFile.getFullPath(), type);
            } else if (Textures.exists(textureFile.getName())) {
                //TODO this is prone to breaking due to the undivided nature of model data/textures, should be proofed a bit more
                logger.debug("Texture '{}' is loaded as fallback based on filename from file {}", textureFile.getName(), textureFile);
                supplier = () -> Textures.ofFile(textureFile.getName(), type);
            } else if (embeddedTextures.values().stream()
                    .map(AbstractTexture.Builder::getType)
                    .anyMatch(builderType -> builderType == type)) {
                logger.debug("Texture '{}' is loaded as fallback from embedded textures based on texture type", textureName);
                supplier = () -> embeddedTextures.values().stream()
                        .filter(texture -> texture.getType() == type)
                        .findFirst()
                        .get()
                        .build();
            } else {
                logger.warn("Failed to find any texture for '{}' at {}", textureName, textureFile);
                continue;
            }

            Texture2D texture = (Texture2D) Loaders.TextureLoader.get().load(textureName, supplier);
            material.addTextures(texture);
            validTextures++;
        }

        logger.trace("{} {} textures loaded", validTextures, type.name().toLowerCase());
    }

    private static void addPropertiesToMaterial(Material.Builder material, AIMaterial aiMaterial) {
        //String properties
        AIString matName = AIString.create();
        if (aiReturn_SUCCESS == aiGetMaterialString(aiMaterial, AI_MATKEY_NAME, aiTextureType_NONE, 0, matName)) {
            material.setProperty(Material.Property.NAME, matName.dataString());
        }

        //Vector4f properties
        AIColor4D colour = AIColor4D.calloc();
        for (Material.Property property : new Material.Property[]{
                COLOUR_BASE, COLOUR_AMBIENT, COLOUR_DIFFUSE, COLOUR_SPECULAR, COLOUR_EMISSIVE
        }) {
            if (aiReturn_SUCCESS == aiGetMaterialColor(aiMaterial, property.ai(), aiTextureType_NONE, 0, colour)) {
                Vector4fc colourProperty = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
                material.setProperty(property, colourProperty);
            }
        }

        //Single value properties
        PointerBuffer propBuffer = BufferUtils.createPointerBuffer(1);
        for (Material.Property property : new Material.Property[]{
                INTENSITY_EMISSIVE, SHININESS, SHININESS_STRENGTH, METALLIC_FACTOR, OPACITY, TRANSPARENCY, BLEND_FUNCTION
        }) {
            if (aiReturn_SUCCESS == aiGetMaterialProperty(aiMaterial, property.ai(), aiTextureType_NONE, 0, propBuffer)) {
                material.setProperty(property, AIMaterialProperty.create(propBuffer.get()).mData().getFloat());
                propBuffer.rewind();
            }
        }

        logger.trace("Loaded properties: {}", material.getProperties());
    }

}
