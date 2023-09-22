package org.etieskrill.engine.graphics.texture;

import glm_.vec2.Vec2i;
import org.etieskrill.engine.graphics.gl.FrameBufferAttachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.etieskrill.engine.graphics.texture.Textures.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.stb.STBImage.*;

/**
 * As this class makes use of the stb_image library, it can decode from all the image formats specified in the
 * official documentation: <a href="https://github.com/nothings/stb/blob/5736b15f7ea0ffb08dd38af21067c314d6a3aae9/stb_image.h#L23-L33">stb_image</a>
 */
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
            super(type);
            this.file = file;
            
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
    
    public static final class BlankBuilder extends Builder {
        BlankBuilder(Vec2i pixelSize, Type type) {
            super(type);
            
            this.textureData = null;
            
            this.pixelSize = pixelSize;
            this.format = Format.RGB;
        }
    
        @Override
        protected void freeResources() {}
    }

    public abstract static class Builder extends AbstractTexture.Builder<Texture2D> {
        Builder(Type type) {
            super(type);
        }

        public Texture2D build() {
            if (format == null) format = Format.NONE;

            String source = "unknown source";
            if (this instanceof Texture2D.FileBuilder builder) source = builder.file;
            else if (this instanceof Texture2D.BlankBuilder) source = "generator";
            logger.debug("Loading {}x{} {}-bit {} texture from {}", pixelSize.getX(), pixelSize.getY(),
                    NR_BITS_PER_COLOUR_CHANNEL * format.getChannels(), type.name().toLowerCase(), source);

            Texture2D texture = new Texture2D(this, 1);

            freeResources();

            return texture;
        }
    }
    
    //TODO this is specifically for tex2d, so declare it as such
    private Texture2D(Builder builder, Integer asdf) {
        this(builder);
    
        int glFormat = format.toGLFormat();
        
        bind(0);
        //TODO currently, the internal format and the format are set to the same value, which seems to work so far
        glTexImage2D(GL_TEXTURE_2D, 0, glFormat, pixelSize.getX(), pixelSize.getY(),
                0, glFormat, GL_UNSIGNED_BYTE, builder.textureData);
    
        if (builder.mipMaps) glGenerateMipmap(builder.target.gl());
    }
    
    Texture2D(Builder builder) {
        super(builder);
        this.pixelSize = builder.pixelSize;
    }

    public Vec2i getPixelSize() {
        return pixelSize;
    }

}
