package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.Disposable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL33C;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.MissingResourceException;

import static org.lwjgl.stb.STBImage.*;

public class Texture implements Disposable {

    private final int textureID;
    private final int pixelWidth, pixelHeight, colourChannels;
    
    /**
     * As this class makes use of the stb_image library, it can decode to all the image formats specified in the
     * official documentation: <a href="https://github.com/nothings/stb/blob/master/stb_image.h">stb_image</a>
     *
     * @param file name of the texture file relative to the resources/textures folder
     */
    public Texture(String file) {
        file = "Engine/src/main/resources/textures/" + file;
        
        textureID = generateGLTexture();
        
        GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, textureID);

        IntBuffer bufferWidth = BufferUtils.createIntBuffer(1),
                bufferHeight = BufferUtils.createIntBuffer(1),
                bufferColourChannels = BufferUtils.createIntBuffer(1);

        stbi_set_flip_vertically_on_load(true);
        ByteBuffer textureData = stbi_load(file, bufferWidth, bufferHeight, bufferColourChannels, 0);
        pixelWidth = bufferWidth.get();
        pixelHeight = bufferHeight.get();
        colourChannels = bufferColourChannels.get();

        if (textureData == null || !textureData.hasRemaining()) {
            throw new MissingResourceException(stbi_failure_reason(), "Texture", file);
        }
        
        int format = switch (colourChannels) {
            case 3 -> GL33C.GL_RGB;
            case 4 -> GL33C.GL_RGBA;
            default -> throw new IllegalStateException("Unexpected colour format: " + colourChannels + " channels");
        };
        
        GL33C.glTexImage2D(GL33C.GL_TEXTURE_2D, 0, GL33C.GL_RGB, pixelWidth, pixelHeight,
                0, format, GL33C.GL_UNSIGNED_BYTE, textureData);

        stbi_image_free(textureData);

        GL33C.glGenerateMipmap(GL33C.GL_TEXTURE_2D);
    }

    private int generateGLTexture() {
        IntBuffer textures = BufferUtils.createIntBuffer(1);
        GL33C.glGenTextures(textures);
        if (!textures.hasRemaining())
            throw new IllegalStateException("OpenGL could not generate any textures");
        return textures.get();
    }
    
    public void bind(int unit) {
        GL33C.glActiveTexture(GL33C.GL_TEXTURE0 + unit);
        GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, textureID);
    }

    public int getTextureID() {
        return textureID;
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

    @Override
    public void dispose() {
        GL33C.glDeleteTextures(textureID);
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
