package org.etieskrill.engine.graphics.texture;

import glm_.vec2.Vec2i;
import org.etieskrill.engine.graphics.gl.FrameBufferAttachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static org.etieskrill.engine.graphics.texture.Textures.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.stb.STBImage.*;

public class Texture2D extends AbstractTexture implements FrameBufferAttachment {
    
    private static final Logger logger = LoggerFactory.getLogger(Texture2D.class);

    private final Vec2i pixelSize;
    
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
            
            file = DIRECTORY + file;
            
            TextureData data = loadFileOrDefault(file, type);
            
            textureData = data.getTextureData();
            pixelSize = data.getPixelSize();
            format = data.getFormat();
        }
        
        @Override
        protected void freeResources() {
            stbi_image_free(textureData);
        }
    }
    
    public static final class BufferBuilder extends Builder {
        public BufferBuilder(ByteBuffer buffer, Vec2i pixelSize, Format format) {
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
        BlankBuilder(Vec2i pixelSize) {
            this.textureData = null;
            
            this.pixelSize = pixelSize;
            this.format = Format.RGB;
        }
    
        @Override
        protected void freeResources() {}
    }

    public abstract static class Builder extends AbstractTexture.Builder<Texture2D> {
        protected ByteBuffer textureData;
        
        Builder() {}
    
        @Override
        protected Texture2D bufferTextureData() {
            String source = "unknown source";
            if (this instanceof Texture2D.FileBuilder builder) source = builder.file;
            else if (this instanceof Texture2D.BlankBuilder) source = "generator";
            logger.debug("Loading {}x{} {}-bit {} texture from {}", pixelSize.getX(), pixelSize.getY(),
                    NR_BITS_PER_COLOUR_CHANNEL * format.getChannels(), type.name().toLowerCase(), source);
            
            int glFormat = format.toGLFormat();
    
            Texture2D texture = new Texture2D(this);
            texture.bind(0);
            //TODO currently, the internal format and the format are set to the same value, which seems to work so far
            glTexImage2D(GL_TEXTURE_2D, 0, glFormat, pixelSize.getX(), pixelSize.getY(),
                    0, glFormat, GL_UNSIGNED_BYTE, textureData);
            
            return texture;
        }
    }
    
    Texture2D(Builder builder) {
        super(builder.setTarget(Target.TWO_D));
        this.pixelSize = builder.pixelSize;
    }

    public Vec2i getPixelSize() {
        return pixelSize;
    }

}
