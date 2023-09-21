package org.etieskrill.engine.graphics.texture;

import glm_.vec2.Vec2i;
import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.graphics.gl.FrameBufferAttachment;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.MissingResourceException;

import static org.etieskrill.engine.graphics.texture.Texture.Type.*;
import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.stb.STBImage.*;

/**
 * As this class makes use of the stb_image library, it can decode from all the image formats specified in the
 * official documentation: <a href="https://github.com/nothings/stb/blob/5736b15f7ea0ffb08dd38af21067c314d6a3aae9/stb_image.h#L23-L33">stb_image</a>
 */
public class Texture implements Disposable, FrameBufferAttachment {
    
    public static final int NR_BITS_PER_COLOUR_CHANNEL = 8;
    
    static final String DIRECTORY = "Engine/src/main/resources/textures/";
    static final String DEFAULT_TEXTURE = "pepega.png";
    static final String TRANSPARENT_TEXTURE = "transparent.png";
    
    private static final Logger logger = LoggerFactory.getLogger(Texture.class);
    
    private final int texture;
    private final int pixelWidth, pixelHeight;
    private final Format format;
    private final Type type;
    //TODO create abstract factory with target as factory impls, which will make this target here redundant
    private final Target target;
    
    //TODO implementing phong/pbr: add toggle and have monolithic texture class, or separate them,
    // actually, the type should be outsourced into a composite class such as MaterialTexture
    public enum Type {
        UNKNOWN(aiTextureType_UNKNOWN), DIFFUSE(aiTextureType_DIFFUSE), SPECULAR(aiTextureType_SPECULAR),
        SHININESS(aiTextureType_SHININESS), HEIGHT(aiTextureType_HEIGHT), EMISSIVE(aiTextureType_EMISSIVE);
        
        private final int aiType;
    
        Type(int aiType) {
            this.aiType = aiType;
        }
        
        public static Type fromAI(int aiTextureType) {
            for (Type type : Type.values())
                if (type.toAI() == aiTextureType) return type;
            return UNKNOWN;
        }
    
        public int toAI() {
            return aiType;
        }
    }
    
    public enum Format {
        NONE(GL_NONE, 0),
        GRAY(GL_RED, 1),
        GA(GL_RG, 2), //TODO GA == gray/alpha not very intuitive, find better name
        RGB(GL_RGB, 3),
        RGBA(GL_RGBA, 4),
        DEPTH(GL_DEPTH_COMPONENT, 1),
        STENCIL(GL_STENCIL_INDEX, 1);
    
        private final int glFormat;
        private final int channels;
        
        Format(int glFormat, int channels) {
            this.glFormat = glFormat;
            this.channels = channels;
        }
        
        public static Format fromPreferredColourChannels(int colourChannels) {
            return switch (colourChannels) {
                case 1 -> GRAY;
                case 2 -> GA;
                case 3 -> RGB;
                case 4 -> RGBA;
                default -> throw new IllegalStateException("Unexpected colour format: " + colourChannels + " channels");
            };
        }
        
        public int toGLFormat() {
            return glFormat;
        }
    
        public int getChannels() {
            return channels;
        }
    }
    
    public enum Target { //TODO more types
        TWO_D(GL_TEXTURE_2D),
        CUBEMAP(GL_TEXTURE_CUBE_MAP);
        
        private final int glTarget;
    
        Target(int glTarget) {
            this.glTarget = glTarget;
        }
    
        public int gl() {
            return glTarget;
        }
    }
    
    public enum MinFilter {
        NEAREST(GL_NEAREST),
        LINEAR(GL_LINEAR),
    
        NEAREST_NEAREST(GL_NEAREST_MIPMAP_NEAREST),
        BILINEAR(GL_LINEAR_MIPMAP_NEAREST),
        NEAREST_LINEAR(GL_NEAREST_MIPMAP_LINEAR),
        TRILINEAR(GL_LINEAR_MIPMAP_LINEAR);
        
        private final int glFilter;
    
        MinFilter(int glFilter) {
            this.glFilter = glFilter;
        }
    
        public int gl() {
            return glFilter;
        }
    }
    
    /**
     * Nearest is said to generally be faster, though linear is in most cases worth the cost.
     */
    public enum MagFilter {
        NEAREST(GL_NEAREST),
        LINEAR(GL_LINEAR);
        
        private final int glFilter;
    
        MagFilter(int glFilter) {
            this.glFilter = glFilter;
        }
    
        public int gl() {
            return glFilter;
        }
    }
    
    public enum Wrapping {
        REPEAT(GL_REPEAT),
        CLAMP_TO_EDGE(GL_CLAMP_TO_EDGE),
        CLAMP_TO_BORDER(GL_CLAMP_TO_BORDER);
        
        private final int glWrapping;
    
        Wrapping(int glWrapping) {
            this.glWrapping = glWrapping;
        }
    
        public int gl() {
            return glWrapping;
        }
    }
    
    public static Texture ofFile(String file, Type type) {
        return new FileBuilder(file, type).build();
    }
    
    public static Texture genBlank(Vec2i size, Format format) {
        Texture texture = new BlankBuilder(size, DIFFUSE).build();
        
        texture.bind(0);
        glTexImage2D(GL_TEXTURE_2D, 0, format.toGLFormat(), size.getX(), size.getY(),
                0, format.toGLFormat(), GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        return texture;
    }
    
    public static final class FileBuilder extends Builder<Texture> {
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
            pixelSize = data.pixelSize;
            format = data.getFormat();
        }
        
        @Override
        protected void freeResources() {
            stbi_image_free(textureData);
        }
    }
    
    static class TextureData {
        private final ByteBuffer textureData;
        private final Vec2i pixelSize;
        private final Format format;
        
        public TextureData(ByteBuffer textureData, Vec2i pixelSize, Format format) {
            this.textureData = textureData;
            this.pixelSize = pixelSize;
            this.format = format;
        }
    
        public ByteBuffer getTextureData() {
            return textureData;
        }
    
        public Vec2i getPixelSize() {
            return pixelSize;
        }
    
        public Format getFormat() {
            return format;
        }
    }
    
    static TextureData loadFileOrDefault(String file, Type type) {
        TextureData data;
        try {
            data = Texture.loadFile(file);
        } catch (MissingResourceException e) {
            logger.info("Texture {} could not be loaded, using placeholder:\n{}", file, stbi_failure_reason());
            file = DIRECTORY + (type == DIFFUSE || type == UNKNOWN ? DEFAULT_TEXTURE : TRANSPARENT_TEXTURE);
            data = Texture.loadFile(file);
        }
        return data;
    }
    
    static TextureData loadFile(String file) {
        IntBuffer bufferWidth = BufferUtils.createIntBuffer(1),
                bufferHeight = BufferUtils.createIntBuffer(1),
                bufferColourChannels = BufferUtils.createIntBuffer(1);

        //stbi_set_flip_vertically_on_load(true); the uv coords are already flipped while loading the models
        ByteBuffer textureData = stbi_load(file, bufferWidth, bufferHeight, bufferColourChannels, 0);
        if (textureData == null || !textureData.hasRemaining())
            throw new MissingResourceException("Texture %s could not be loaded:\n%s".formatted(file, stbi_failure_reason()),
                    Texture.class.getSimpleName(), file);
    
        int pixelWidth = bufferWidth.get();
        int pixelHeight = bufferHeight.get();
        Format format = Format.fromPreferredColourChannels(bufferColourChannels.get());
        
        return new TextureData(textureData, new Vec2i(pixelWidth, pixelHeight), format);
    }
    
    public static final class BlankBuilder extends Builder {
        private BlankBuilder(Vec2i pixelSize, Type type) {
            super(type);
            
            this.textureData = null;
            
            this.pixelSize = pixelSize;
            this.format = Format.RGB;
        }
    
        @Override
        protected void freeResources() {}
    }
    
    public static abstract class Builder<T extends Texture> {
        protected static final int INVALID_PIXEL_SIZE = -1;
        
        protected final Type type;
        
        protected Target target = Target.TWO_D;
        protected MinFilter minFilter = MinFilter.TRILINEAR;
        protected MagFilter magFilter = MagFilter.LINEAR;
        protected Wrapping wrapping = Wrapping.REPEAT;
    
        protected ByteBuffer textureData;
    
        //TODO for inheriting builders it is kinda useful if properties are not set/have invalid values from here
        protected Vec2i pixelSize = new Vec2i(INVALID_PIXEL_SIZE);
        protected Format format = null;
    
        protected boolean autoSwizzleMask = true;
        protected boolean mipMaps = true;
        
        Builder(Type type) {
            this.type = type;
        }
    
        public Builder<T> setTarget(Target target) {
            this.target = target;
            return this;
        }
    
        public Builder<T> disableAutoSwizzleMask() {
            this.autoSwizzleMask = false;
            return this;
        }
        
        public Builder<T> noMipMaps() {
            if (!(minFilter == MinFilter.NEAREST || minFilter == MinFilter.LINEAR))
                throw new IllegalStateException("minFilter setting " + minFilter.name() + " requires mipmaps");
            this.mipMaps = false;
            return this;
        }
        
        public Builder<T> setMipMapping(MinFilter minFilter, MagFilter magFilter) {
            if (!mipMaps && (minFilter == MinFilter.NEAREST || minFilter == MinFilter.LINEAR))
                throw new IllegalArgumentException("Mipmapping was disabled (must use NEAREST or LINEAR for minFilter)");
            if (mipMaps && (minFilter == MinFilter.NEAREST_NEAREST || minFilter == MinFilter.NEAREST_LINEAR
                    || minFilter == MinFilter.BILINEAR || minFilter == MinFilter.TRILINEAR))
                throw new IllegalArgumentException("Mipmapping is enabled (must use BILINEAR, TRILINEAR, " +
                        "NEAREST_NEAREST or NEAREST_LINEAR for minFilter)");
            this.minFilter = minFilter;
            this.magFilter = magFilter;
            return this;
        }
        
        public Builder<T> setWrapping(Wrapping wrapping) {
            this.wrapping = wrapping;
            return this;
        }
        
        public T build() {
            if (format == null) format = Format.NONE;
            
            String source = "unknown source";
            if (this instanceof FileBuilder builder) source = builder.file;
            else if (this instanceof BlankBuilder) source = "generator";
            logger.debug("Loading {}x{} {}-bit {} texture from {}", pixelSize.getX(), pixelSize.getY(),
                    NR_BITS_PER_COLOUR_CHANNEL * format.getChannels(), type.name().toLowerCase(), source);
            
            Texture texture = new Texture(this, 1);
    
            freeResources();
            
            return (T) texture;
        }
        
        protected abstract void freeResources();
    }
    
    //TODO this is specifically for tex2d, so declare it as such
    private Texture(Builder<? extends Texture> builder, Integer asdf) {
        this(builder);
    
        int glFormat = format.toGLFormat();
        
        bind(0);
        //TODO currently, the internal format and the format are set to the same value, which seems to work so far
        glTexImage2D(GL_TEXTURE_2D, 0, glFormat, pixelWidth, pixelHeight,
                0, glFormat, GL_UNSIGNED_BYTE, builder.textureData);
    
        if (builder.mipMaps) glGenerateMipmap(builder.target.gl());
    }
    
    Texture(Builder<? extends Texture> builder) {
        this.pixelWidth = builder.pixelSize.getX();
        this.pixelHeight = builder.pixelSize.getY();
        
        this.format = builder.format;
        this.type = builder.type;
        this.target = builder.target;
        
        this.texture = glGenTextures();
        bind(0);
    
        glTexParameteri(target.gl(), GL_TEXTURE_MIN_FILTER, builder.minFilter.gl());
        glTexParameteri(target.gl(), GL_TEXTURE_MAG_FILTER, builder.magFilter.gl());
    
        glTexParameteri(target.gl(), GL_TEXTURE_WRAP_S, builder.wrapping.gl());
        glTexParameteri(target.gl(), GL_TEXTURE_WRAP_T, builder.wrapping.gl());
        glTexParameteri(target.gl(), GL_TEXTURE_WRAP_R, builder.wrapping.gl());
    
        if (builder.autoSwizzleMask) {
            int[] swizzleMask = switch (builder.format.getChannels()) {
                case 1 -> new int[] {GL_RED, GL_RED, GL_RED, GL_ONE};
                case 2 -> new int[] {GL_RED, GL_RED, GL_RED, GL_GREEN};
                case 3, 4 -> new int[] {GL_RED, GL_GREEN, GL_BLUE, GL_ALPHA};
                default -> throw new IllegalStateException("Unexpected colour format: " + builder.format.getChannels() + " channels");
            };
        
            glTexParameteriv(target.gl(), GL_TEXTURE_SWIZZLE_RGBA, swizzleMask);
        }
    }
    
    //TODO rework binding system; perhaps standard bind to 0, and only use units in specific cases like rendering
    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, texture);
    }
    
    public void unbind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    
    public static void unbindAllTextureUnits() {
        for (int i = 0; i < 32; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    }

    //TODO create private wrapper class which opens this to classes such as FrameBuffer
    @Deprecated
    public int getID() {
        return texture;
    }
    
    public Type getType() {
        return type;
    }
    
    public Target getTarget() {
        return target;
    }
    
    public int getPixelWidth() {
        return pixelWidth;
    }

    public int getPixelHeight() {
        return pixelHeight;
    }
    
    public Format getFormat() {
        return format;
    }
    
    //TODO is there any way to make this less stateful / just more better
    private boolean wasAlreadyDisposed = false;
    
    @Override
    public void dispose() {
        if (wasAlreadyDisposed) return;
        glDeleteTextures(texture);
        wasAlreadyDisposed = true;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Texture texture = (Texture) o;
        
        if (this.texture != texture.texture) return false;
        if (pixelWidth != texture.pixelWidth) return false;
        if (pixelHeight != texture.pixelHeight) return false;
        return format.equals(texture.format);
    }
    
    @Override
    public int hashCode() {
        int result = texture;
        result = 31 * result + pixelWidth;
        result = 31 * result + pixelHeight;
        result = 31 * result + format.getChannels();
        return result;
    }
    
}
