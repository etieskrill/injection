package org.etieskrill.engine.graphics.texture;

import org.etieskrill.engine.common.ResourceLoadException;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.util.ResourceReader;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.MissingResourceException;

import static org.etieskrill.engine.config.ResourcePaths.TEXTURE_CUBEMAP_PATH;
import static org.etieskrill.engine.config.ResourcePaths.TEXTURE_PATH;
import static org.etieskrill.engine.graphics.texture.AbstractTexture.Type.DIFFUSE;
import static org.etieskrill.engine.graphics.texture.AbstractTexture.Type.UNKNOWN;
import static org.lwjgl.stb.STBImage.stbi_failure_reason;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;

public class Textures {

    public static final int NR_BITS_PER_COLOUR_CHANNEL = 8;

    static final String DEFAULT_TEXTURE = "pepega.png";
    static final String TRANSPARENT_TEXTURE = "transparent.png"; //TODO replace with blank generated texture

    private static final Logger logger = LoggerFactory.getLogger(Textures.class);

    private Textures() {
        //Not intended for instantiation
    }

    public static Texture2D genBlank(Vector2ic size, AbstractTexture.Format format) {
        return new Texture2D.BlankBuilder(size)
                .setType(DIFFUSE)
                .setFormat(format)
                .setMipMapping(AbstractTexture.MinFilter.LINEAR, AbstractTexture.MagFilter.LINEAR)
                .setWrapping(AbstractTexture.Wrapping.CLAMP_TO_EDGE)
                .build();
    }

    static AbstractTexture.TextureData loadFileOrDefault(String file, AbstractTexture.Type type) {
        AbstractTexture.TextureData data;
        try {
            data = Textures.loadFile(file, type);
        } catch (MissingResourceException | ResourceLoadException e) {
            String reason = stbi_failure_reason();
            logger.info("Texture {} could not be loaded, using placeholder because:\n\t{}",
                    file, reason != null ? reason : e.getMessage());
            file = type == DIFFUSE || type == UNKNOWN ? DEFAULT_TEXTURE : TRANSPARENT_TEXTURE;
            try {
                data = Textures.loadFile(file, type);
            } catch (MissingResourceException | ResourceLoadException ex) {
                throw new RuntimeException("Failed to load default texture: this is an engine-internal error", ex);
            }
        }
        return data;
    }

    static AbstractTexture.TextureData loadFile(String file, AbstractTexture.Type type) {
        IntBuffer bufferWidth = BufferUtils.createIntBuffer(1);
        IntBuffer bufferHeight = BufferUtils.createIntBuffer(1);
        IntBuffer bufferColourChannels = BufferUtils.createIntBuffer(1);

        //stbi_set_flip_vertically_on_load(true); the uv coords are already flipped while loading the models
        ByteBuffer textureData = stbi_load_from_memory(
                ResourceReader.getRawClasspathResource(file.contains("/" + TEXTURE_CUBEMAP_PATH) ? file : TEXTURE_PATH + file),
                bufferWidth, bufferHeight, bufferColourChannels, 0);
        if (textureData == null || !textureData.hasRemaining())
            throw new MissingResourceException("Texture %s could not be loaded:%n%s".formatted(file, stbi_failure_reason()),
                    Texture2D.class.getSimpleName(), file);

        int pixelWidth = bufferWidth.get();
        int pixelHeight = bufferHeight.get();
        AbstractTexture.Format format = AbstractTexture.Format.fromChannelsAndType(bufferColourChannels.get(), type);

        return new AbstractTexture.TextureData(textureData, new Vector2i(pixelWidth, pixelHeight), format);
    }

    public static Texture2D ofFile(String file) {
        return new Texture2D.FileBuilder(file).build();
    }

    public static Texture2D ofFile(String file, AbstractTexture.Type type) {
        return new Texture2D.FileBuilder(file, type).build();
    }
    
    public static CubeMapTexture getSkybox(String name) {
        return (CubeMapTexture) Loaders.TextureLoader.get().load("cubemap/" + name, () ->
                CubeMapTexture.CubemapTextureBuilder.get(name)
                        .setMipMapping(AbstractTexture.MinFilter.LINEAR, AbstractTexture.MagFilter.LINEAR)
                        .setWrapping(AbstractTexture.Wrapping.CLAMP_TO_EDGE)
                        .build());
    }

}
