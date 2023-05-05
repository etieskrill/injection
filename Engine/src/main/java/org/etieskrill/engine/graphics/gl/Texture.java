package org.etieskrill.engine.graphics.gl;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL33C;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.MissingResourceException;

import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load;

public class Texture {

    private final int textureID;
    private final int pixelWidth, pixelHeight, colourChannels;

    public Texture(String file) {
        textureID = generateGLTexture();

        GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, textureID);

        IntBuffer bufferWidth = BufferUtils.createIntBuffer(1),
                bufferHeight = BufferUtils.createIntBuffer(1),
                bufferColourChannels = BufferUtils.createIntBuffer(1);

        ByteBuffer textureData = stbi_load(file, bufferWidth, bufferHeight, bufferColourChannels, 0);
        pixelWidth = bufferWidth.get();
        pixelHeight = bufferHeight.get();
        colourChannels = bufferColourChannels.get();

        if (textureData == null || !textureData.hasRemaining()) {
            throw new MissingResourceException("Error loading texture data", "Texture", file);
        }

        GL33C.glTexImage2D(GL33C.GL_TEXTURE_2D, 0, GL33C.GL_RGB, pixelWidth, pixelHeight,
                0, GL33C.GL_RGB, GL33C.GL_UNSIGNED_BYTE, textureData);

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

    public void dispose() {
        GL33C.glDeleteTextures(textureID);
    }

}
