package org.etieskrill.engine.graphics.texture;

import glm_.vec2.Vec2i;
import org.etieskrill.engine.util.Loaders;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.MissingResourceException;

import static org.etieskrill.engine.graphics.texture.AbstractTexture.DIRECTORY;
import static org.etieskrill.engine.graphics.texture.AbstractTexture.Type.DIFFUSE;
import static org.etieskrill.engine.graphics.texture.AbstractTexture.Type.UNKNOWN;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.stb.STBImage.stbi_failure_reason;
import static org.lwjgl.stb.STBImage.stbi_load;

public class Textures {

    public static final int NR_BITS_PER_COLOUR_CHANNEL = 8;

    static final String DEFAULT_TEXTURE = "pepega.png";
    static final String TRANSPARENT_TEXTURE = "transparent.png";

    private static final Logger logger = LoggerFactory.getLogger(Textures.class);

    public static Texture2D genBlank(Vec2i size, AbstractTexture.Format format) {
        Texture2D texture = new Texture2D.BlankBuilder(size, DIFFUSE).build();

        texture.bind(0);
        glTexImage2D(GL_TEXTURE_2D, 0, format.toGLFormat(), size.getX(), size.getY(),
                0, format.toGLFormat(), GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        return texture;
    }

    static AbstractTexture.TextureData loadFileOrDefault(String file, AbstractTexture.Type type) {
        AbstractTexture.TextureData data;
        try {
            data = Textures.loadFile(file);
        } catch (MissingResourceException e) {
            logger.info("Texture {} could not be loaded, using placeholder:\n{}", file, stbi_failure_reason());
            file = DIRECTORY + (type == DIFFUSE || type == UNKNOWN ? DEFAULT_TEXTURE : TRANSPARENT_TEXTURE);
            data = Textures.loadFile(file);
        }
        return data;
    }

    static AbstractTexture.TextureData loadFile(String file) {
        IntBuffer bufferWidth = BufferUtils.createIntBuffer(1),
                bufferHeight = BufferUtils.createIntBuffer(1),
                bufferColourChannels = BufferUtils.createIntBuffer(1);

        //stbi_set_flip_vertically_on_load(true); the uv coords are already flipped while loading the models
        ByteBuffer textureData = stbi_load(file, bufferWidth, bufferHeight, bufferColourChannels, 0);
        if (textureData == null || !textureData.hasRemaining())
            throw new MissingResourceException("Texture %s could not be loaded:\n%s".formatted(file, stbi_failure_reason()),
                    Texture2D.class.getSimpleName(), file);

        int pixelWidth = bufferWidth.get();
        int pixelHeight = bufferHeight.get();
        AbstractTexture.Format format = AbstractTexture.Format.fromPreferredColourChannels(bufferColourChannels.get());

        return new AbstractTexture.TextureData(textureData, new Vec2i(pixelWidth, pixelHeight), format);
    }

    public static Texture2D ofFile(String file, AbstractTexture.Type type) {
        return new Texture2D.FileBuilder(file, type).build();
    }
    
    public static CubeMapTexture getSkybox(String name) {
        return (CubeMapTexture) Loaders.TextureLoader.get().load("cubemap/" + name, () ->
                CubeMapTexture.CubemapTextureBuilder.get(name)
                        .setMipMapping(Texture2D.MinFilter.LINEAR, Texture2D.MagFilter.LINEAR)
                        .setWrapping(Texture2D.Wrapping.CLAMP_TO_EDGE)
                        .build());
    }

}
