package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.Disposable;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.MissingResourceException;
import java.util.function.Supplier;

import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.stb.STBImage.*;

/**
 * As this class makes use of the stb_image library, it can decode from all the image formats specified in the
 * official documentation: <a href="https://github.com/nothings/stb/blob/5736b15f7ea0ffb08dd38af21067c314d6a3aae9/stb_image.h#L23-L33">stb_image</a>
 */
public class Texture implements Disposable {
    
    public static final int NR_BITS_PER_COLOUR_CHANNEL = 8;
    
    private static final String directory = "Engine/src/main/resources/textures/";
    //TODO what to do if not all textures could be loaded; only load single diffuse placeholder,
    // or if diffuse is present only that - dunno, choices, choices
    private static final Supplier<Texture> DEFAULT_TEXTURE = () -> Texture.ofFile("pepega.png", TextureType.DIFFUSE);
    private static final Supplier<Texture> TRANSPARENT_TEXTURE = () -> Texture.ofFile("transparent.png", TextureType.UNKNOWN);
    
    private static final Logger logger = LoggerFactory.getLogger(Texture.class);
    
    private final String file;
    
    private final int textureID;
    private final int pixelWidth, pixelHeight, colourChannels;
    private final TextureType type;
    
    //TODO implementing phong/pbr: add toggle and have monolithic texture class, or separate them,
    // actually, the type should be outsourced into a composite class such as MaterialTexture
    public enum TextureType {
        UNKNOWN,
        DIFFUSE,
        SPECULAR,
        SHININESS,
        HEIGHT,
        EMISSIVE;
        
        public static TextureType fromAITextureType(int aiTextureType) {
            return switch (aiTextureType) {
                case aiTextureType_DIFFUSE -> DIFFUSE;
                case aiTextureType_SPECULAR -> SPECULAR;
                case aiTextureType_EMISSIVE -> EMISSIVE;
                case aiTextureType_HEIGHT -> HEIGHT;
                case aiTextureType_SHININESS -> SHININESS;
                default -> UNKNOWN;
            };
        }
    
        public int toAITextureType() {
            return switch (this) {
                case DIFFUSE -> aiTextureType_DIFFUSE;
                case SPECULAR -> aiTextureType_SPECULAR;
                case EMISSIVE -> aiTextureType_EMISSIVE;
                case HEIGHT -> aiTextureType_HEIGHT;
                case SHININESS -> aiTextureType_SHININESS;
                default -> aiTextureType_UNKNOWN;
            };
        }
    }
    
    /**
     * Reads image attributes from the specified file and generates a GL texture object.
     *
     * @param file name of the texture file relative to the resources/textures folder
     * @param type the type of texture, if any
     */
    public static Texture ofFile(String file, TextureType type) {
        try {
            return new Texture(file, type);
        } catch (MissingResourceException e) {
            logger.debug("Texture could not be loaded, using placeholder: ", e);
            return type == TextureType.DIFFUSE ? DEFAULT_TEXTURE.get() : TRANSPARENT_TEXTURE.get();
        }
    }
    
    /**
     * Reads image attributes from the specified file and generates a GL texture object.
     * @param file name of the texture file relative to the resources/textures folder
     */
    public static Texture ofFile(String file) {
        return ofFile(file, TextureType.UNKNOWN);
    }
    
    public static Texture getDefault() {
        return DEFAULT_TEXTURE.get();
    }
    
    private Texture(String file, TextureType type) {
        if (type == null) {
            type = TextureType.UNKNOWN;
            logger.trace("Texture {} has no type specified", file);
        }
    
        this.file = file;
        this.type = type;
        
        file = directory + this.file;
        
        textureID = generateGLTexture();
        glBindTexture(GL_TEXTURE_2D, textureID);

        IntBuffer bufferWidth = BufferUtils.createIntBuffer(1),
                bufferHeight = BufferUtils.createIntBuffer(1),
                bufferColourChannels = BufferUtils.createIntBuffer(1);

        //stbi_set_flip_vertically_on_load(true); the uv coords are already flipped while loading the models
        ByteBuffer textureData = stbi_load(file, bufferWidth, bufferHeight, bufferColourChannels, 0);
        if (textureData == null || !textureData.hasRemaining()) {
            throw new MissingResourceException(stbi_failure_reason(), getClass().getSimpleName(), file);
        }
        
        pixelWidth = bufferWidth.get();
        pixelHeight = bufferHeight.get();
        colourChannels = bufferColourChannels.get();
    
        //TODO currently, the internal format and the format are set to the same value, which seems to work so far
        int format = switch (colourChannels) {
            case 1 -> GL_RED; //GL_ALPHA does NOT work, this caused me quite a bit of pain
            case 2 -> GL_RG;
            case 3 -> GL_RGB;
            case 4 -> GL_RGBA;
            default -> throw new IllegalStateException("Unexpected colour format: " + colourChannels + " channels");
        };
    
        //TODO adjust this if weird colour mappings happen to textures in shaders
        int[] swizzleMask = switch (colourChannels) {
            case 1 -> new int[] {GL_RED, GL_RED, GL_RED, GL_ONE};
            case 2 -> new int[] {GL_RED, GL_RED, GL_RED, GL_GREEN};
            case 3, 4 -> new int[] {GL_RED, GL_GREEN, GL_BLUE, GL_ALPHA};
            default -> throw new IllegalStateException("Unexpected colour format: " + colourChannels + " channels");
        };
        
        glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_RGBA, swizzleMask);
        
        glTexImage2D(GL_TEXTURE_2D, 0, format, pixelWidth, pixelHeight,
                0, format, GL_UNSIGNED_BYTE, textureData);
        logger.debug("Loaded {}x{} {}-bit {} texture from {}",
                pixelWidth, pixelHeight, NR_BITS_PER_COLOUR_CHANNEL * colourChannels, type.name().toLowerCase(), file);

        stbi_image_free(textureData);

        glGenerateMipmap(GL_TEXTURE_2D);
    }

    private int generateGLTexture() {
        IntBuffer textures = BufferUtils.createIntBuffer(1);
        glGenTextures(textures);
        if (!textures.hasRemaining())
            throw new IllegalStateException("OpenGL could not generate any textures");
        return textures.get();
    }
    
    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }
    
    public String getFile() {
        return file;
    }
    
    public TextureType getType() {
        return type;
    }
    
    public int getPixelWidth() {
        return pixelWidth;
    }

    public int getPixelHeight() {
        return pixelHeight;
    }

    public int getColourChannels() {
        return colourChannels;
    }

    //TODO is there any way to make this less stateful / just more better
    private boolean wasAlreadyDisposed = false;
    
    @Override
    public void dispose() {
        if (wasAlreadyDisposed) return;
        glDeleteTextures(textureID);
        wasAlreadyDisposed = true;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Texture texture = (Texture) o;
        
        if (textureID != texture.textureID) return false;
        if (pixelWidth != texture.pixelWidth) return false;
        if (pixelHeight != texture.pixelHeight) return false;
        return colourChannels == texture.colourChannels;
    }
    
    @Override
    public int hashCode() {
        int result = textureID;
        result = 31 * result + pixelWidth;
        result = 31 * result + pixelHeight;
        result = 31 * result + colourChannels;
        return result;
    }
    
}
