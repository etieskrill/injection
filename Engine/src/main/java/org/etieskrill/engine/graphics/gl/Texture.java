package org.etieskrill.engine.graphics.gl;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL33C;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.MissingResourceException;

import static org.lwjgl.stb.STBImage.*;

public class Texture {

    private final int textureID;
    private final int pixelWidth, pixelHeight, colourChannels;
    
    
    /**
     * As this class makes use of the stb_image library, it can decode to all the image formats specified in the
     * official documentation: <a href="https://github.com/nothings/stb/blob/master/stb_image.h">stb_image</a>
     *
     * @param file name of the texture file relative to the resources/textures folder
     */
    public Texture(String file, int unit) {
        if (unit < 0 || unit > 15)
            throw new IllegalArgumentException("Texture unit " + unit + " is out of the valid range");
        
        file = "Engine/src/main/resources/textures/" + file;
        
        textureID = generateGLTexture();
        
        //GL33C.glActiveTexture(GL13C.GL_TEXTURE0 + unit);
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
        
        //TODO the colour format is apparently not resolved in accordance with the file format by the bloody library, so what is it for then?
        int format = file.contains("jpg") ? GL33C.GL_RGB : GL33C.GL_RGBA;
        
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

    public void dispose() {
        GL33C.glDeleteTextures(textureID);
    }

}
