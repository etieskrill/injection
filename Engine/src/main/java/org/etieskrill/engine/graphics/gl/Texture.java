package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.Disposable;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.MissingResourceException;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.stb.STBImage.*;

/**
 * As this class makes use of the stb_image library, it can decode from all the image formats specified in the
 * official documentation: <a href="https://github.com/nothings/stb/blob/master/stb_image.h">stb_image</a>
 */
public class Texture implements Disposable {
    
    private static final String directory = "Engine/src/main/resources/textures/";
    private static final Supplier<Texture> DEFAULT_TEXTURE = () -> Texture.ofFile("pepega.png");
    
    private static final Logger logger = LoggerFactory.getLogger(Texture.class);
    
    private final String file;
    
    private final int textureID;
    private final int pixelWidth, pixelHeight, colourChannels;
    private final TextureType type;
    
    public enum TextureType {
        UNKNOWN,
        DIFFUSE,
        SPECULAR,
        SHININESS,
        HEIGHT,
        EMISSIVE
    }
    
    /**
     * Reads image attributes from the specified file and generates a GL texture object.
     *
     * @param file name of the texture file relative to the resources/textures folder
     * @param type the type of texture, if any
     */
    public static Texture ofFile(String file, TextureType type) {
        logger.debug("Loading {} texture from {}", type.name().toLowerCase(), file);
        
        try {
            return new Texture(file, type);
        } catch (MissingResourceException e) {
            logger.debug("Texture could not be loaded, using placeholder: ", e);
            return DEFAULT_TEXTURE.get();
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

        stbi_set_flip_vertically_on_load(true);
        ByteBuffer textureData = stbi_load(file, bufferWidth, bufferHeight, bufferColourChannels, 0);
        if (textureData == null || !textureData.hasRemaining()) {
            throw new MissingResourceException(stbi_failure_reason(), getClass().getSimpleName(), file);
        }
        
        pixelWidth = bufferWidth.get();
        pixelHeight = bufferHeight.get();
        colourChannels = bufferColourChannels.get();
        
        int format = switch (colourChannels) {
            case 1 -> GL_ALPHA;
            case 3 -> GL_RGB;
            case 4 -> GL_RGBA;
            default -> throw new IllegalStateException("Unexpected colour format: " + colourChannels + " channels");
        };
        
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, pixelWidth, pixelHeight,
                0, format, GL_UNSIGNED_BYTE, textureData);

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

    public int getTextureID() {
        return textureID;
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

    //TODO is there any way to make this less stateful / just better
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
