package org.etieskrill.engine.graphics.texture;

import org.etieskrill.engine.graphics.gl.FrameBufferAttachment;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static org.etieskrill.engine.graphics.texture.Textures.NR_BITS_PER_COLOUR_CHANNEL;
import static org.etieskrill.engine.graphics.texture.Textures.loadFileOrDefault;
import static org.lwjgl.opengl.GL33C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL33C.glTexImage2D;
import static org.lwjgl.stb.STBImage.stbi_image_free;

public class Texture2D extends AbstractTexture implements FrameBufferAttachment {
    
    private static final Logger logger = LoggerFactory.getLogger(Texture2D.class);

    private final Vector2ic size;
    
    public static final class FileBuilder extends Builder {
        private final String file;
    
        /**
         * Reads image attributes from the specified file and constructs a texture builder.
         *
         * @param file name of the texture file relative to the resources/textures folder
         * @param type the type of texture, if any
         */
        public FileBuilder(String file, Type type) {
            this.file = file;
            this.type = type;

            TextureData data = loadFileOrDefault(file, type);

            textureData = data.textureData();
            pixelSize = data.pixelSize();
            format = data.format();
        }
        
        @Override
        protected void freeResources() {
            stbi_image_free(textureData);
        }
    }
    
    public static final class BufferBuilder extends Builder {
        public BufferBuilder(@Nullable ByteBuffer buffer, Vector2i pixelSize, Format format) {
            this.textureData = buffer;
            this.pixelSize = pixelSize;
            this.format = format;
        }
    
        @Override
        protected void freeResources() {
            //TODO free buffer if possible
        }
    }
    
    public static final class BlankBuilder extends Builder {
        public BlankBuilder(Vector2ic pixelSize) {
            this.textureData = null;
            this.pixelSize = pixelSize;
            this.format = Format.SRGB;
        }
    
        @Override
        protected void freeResources() {}
    }

    public abstract static class Builder extends AbstractTexture.Builder<Texture2D> {
        protected ByteBuffer textureData;
        
        private Builder() {}
    
        @Override
        protected Texture2D bufferTextureData() {
            logger.debug("Loading {}x{} {}-bit {} {} texture from {}", pixelSize.x(), pixelSize.y(),
                    NR_BITS_PER_COLOUR_CHANNEL * format.getChannels(), format, type.name().toLowerCase(),
                    getClass().getSimpleName());

            Texture2D texture = new Texture2D(this);
            texture.bind(0);
            glTexImage2D(target.gl(), 0, format.toGlInternalFormat(), pixelSize.x(), pixelSize.y(),
                    0, format.toGLFormat(), GL_UNSIGNED_BYTE, textureData);

            return texture;
        }
    }
    
    Texture2D(Builder builder) {
        super(builder.setTarget(Target.TWO_D));
        this.size = builder.pixelSize;
    }

    @Override
    public Vector2ic getSize() {
        return size;
    }

}
