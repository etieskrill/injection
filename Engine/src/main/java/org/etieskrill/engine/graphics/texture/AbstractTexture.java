package org.etieskrill.engine.graphics.texture;

import org.etieskrill.engine.Disposable;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Objects;

import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.opengl.GL33C.*;

/**
 * As this class makes use of the stb_image library, it can decode from all the image formats specified in the
 * official documentation: <a href="https://github.com/nothings/stb/blob/5736b15f7ea0ffb08dd38af21067c314d6a3aae9/stb_image.h#L23-L33">stb_image</a>
 */
public abstract class AbstractTexture implements Disposable {

    protected final int texture;
    protected final Format format;
    protected final Type type;
    protected final Target target;

    private static final Logger logger = LoggerFactory.getLogger(AbstractTexture.class);

    public enum Target { //TODO more types
        TWO_D(Texture2D.class, GL_TEXTURE_2D),
        CUBEMAP(CubeMapTexture.class, GL_TEXTURE_CUBE_MAP);

        private final Class<? extends AbstractTexture> type;
        private final int glTarget;

        Target(Class<? extends AbstractTexture> type, int glTarget) {
            this.type = type;
            this.glTarget = glTarget;
        }

        public Class<? extends AbstractTexture> getType() {
            return type;
        }

        public int gl() {
            return glTarget;
        }
    }

    public enum Format {
        NONE(GL_NONE, GL_NONE, 0),
        ALPHA(GL_RED, GL_RED, 1),
        GRAY(GL_RED, GL_RED, 1),
        GA(GL_RG, GL_RG, 2), //TODO GA == gray/alpha not very intuitive, find better name,
        RGB(GL_RGB, GL_RGB, 3),
        RGBA(GL_RGBA, GL_RGBA, 4),
        SRGB(GL_RGB, GL_SRGB, 3),
        SRGBA(GL_RGBA, GL_SRGB_ALPHA, 4),
        DEPTH(GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT, 1),
        STENCIL(GL_STENCIL_INDEX, GL_STENCIL_INDEX, 1);

        private final int glFormat;
        private final int glInternalFormat;
        private final int channels;

        Format(int glFormat, int glInternalFormat, int channels) {
            this.glFormat = glFormat;
            this.glInternalFormat = glInternalFormat;
            this.channels = channels;
        }

        public static Format fromChannels(int colourChannels) {
            return switch (colourChannels) {
                case 1 -> GRAY;
                case 2 -> GA;
                case 3 -> SRGB;
                case 4 -> SRGBA;
                default -> throw new IllegalStateException("Unexpected colour format: " + colourChannels + " channels");
            };
        }

        public static Format fromChannelsAndType(int colourChannels, Type type) {
            return switch (colourChannels) {
                case 1 -> GRAY;
                case 2 -> GA;
                case 3 -> type == Type.DIFFUSE ? SRGB : RGB;
                case 4 -> type == Type.DIFFUSE ? SRGBA : RGBA;
                default -> throw new IllegalStateException("Unexpected colour format: " + colourChannels + " channels");
            };
        }

        public int toGLFormat() {
            return glFormat;
        }

        public int toGlInternalFormat() {
            return glInternalFormat;
        }

        public int getChannels() {
            return channels;
        }
    }

    /**
     * The minification filtering mode describes the way textures are sampled if a fragment contains several texels.
     * <p>
     * The general process differs from magnification due to the existence of mipmaps. There are now two parameters we
     * can decide on how to interpolate between values; both mipmaps and texels.
     * <p>
     * The {@link MinFilter#NEAREST} and {@link MinFilter#LINEAR} modes ignore mipmapping entirely, and work as their
     * {@link MagFilter MagFilter} equivalents do.
     * <p>
     * Other than those, the following combinations can be achieved:
     * <ul>
     *     <li>{@link MinFilter#NEAREST_NEAREST}, which samples the closest texel from the closest mipmap. This is going
     *         to look both grainy, and give sharp edges when the mipmapping level switches.
     *     <li>{@link MinFilter#BILINEAR}, which interpolates between nearby texels sampled from the closest mipmap.
     *         Textures will look smooth, but the sharp edges on the mipmap borders remain.
     *     <li>{@link MinFilter#NEAREST_LINEAR}, which samples the closest texels from each of the surrounding mipmaps,
     *         and interpolates between those samples. Not used very often, unless grainy textures are intentional.
     *     <li>{@link MinFilter#TRILINEAR}, which interpolates the nearby texels sampled from each of the relevant
     *         mipmaps, and interpolates between the mipmap levels. This is the standard option for most use cases.
     * </ul>
     */
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
     * The magnification filtering mode dictates how textures are sampled if one texel takes up more than one fragment.
     * <ul>
     *     <li>{@link MagFilter#NEAREST} simply samples the closest texel, and will look very grainy, while
     *     <li>{@link MagFilter#LINEAR} interpolates between the closest texels.
     * </ul>
     * Though {@link  MagFilter#NEAREST} is generally going to be slightly faster, {@link MagFilter#LINEAR} is worth
     * the cost in most cases, and is the gold standard.
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

    //TODO implementing phong/pbr: add toggle and have monolithic texture class, or separate them,
    // actually, the type should be outsourced into a composite class such as MaterialTexture
    public enum Type {
        UNKNOWN(aiTextureType_UNKNOWN), DIFFUSE(aiTextureType_DIFFUSE), SPECULAR(aiTextureType_SPECULAR),
        SHININESS(aiTextureType_SHININESS), HEIGHT(aiTextureType_HEIGHT), EMISSIVE(aiTextureType_EMISSIVE),
        NORMAL(aiTextureType_NORMALS);

        private final int aiType;

        Type(int aiType) {
            this.aiType = aiType;
        }

        public static Type ai(int aiTextureType) {
            for (Type type : Type.values())
                if (type.ai() == aiTextureType) return type;
            return UNKNOWN;
        }

        public int ai() {
            return aiType;
        }
    }

    public static abstract class Builder<T extends AbstractTexture> {
        protected static final int INVALID_PIXEL_SIZE = -1;

        //TODO inform whether type is truly part of all texture targets
        protected Type type = Type.UNKNOWN;

        protected Target target = Target.TWO_D;
        protected MinFilter minFilter = MinFilter.TRILINEAR;
        protected MagFilter magFilter = MagFilter.LINEAR;
        protected Wrapping wrapping = Wrapping.REPEAT;

        //TODO for inheriting builders it is kinda useful if properties are not set/have invalid values from here
        protected Vector2ic pixelSize = new Vector2i(INVALID_PIXEL_SIZE);
        protected Format format = null;

        protected boolean autoSwizzleMask = true;
        protected boolean mipMaps = true;

        Builder() {}

        public Builder<T> setType(@NotNull Type type) {
            this.type = type;
            return this;
        }

        public Type getType() {
            return type;
        }

        public Builder<T> setTarget(Target target) {
            this.target = target;
            return this;
        }
    
        public Builder<T> setFormat(Format format) {
            this.format = format;
            return this;
        }
    
        public Builder<T> disableAutoSwizzleMask() {
            this.autoSwizzleMask = false;
            return this;
        }

        public Builder<T> setMipMapping(MinFilter minFilter, MagFilter magFilter) {
            this.mipMaps = minFilter == MinFilter.NEAREST || minFilter == MinFilter.LINEAR;
            this.minFilter = minFilter;
            this.magFilter = magFilter;
            return this;
        }

        public Builder<T> setWrapping(Wrapping wrapping) {
            this.wrapping = wrapping;
            return this;
        }

        public final T build() {
            T texture = bufferTextureData();

            //Wack solution for post-creation method calls
            if (mipMaps) glGenerateMipmap(target.gl());
            
            freeResources();
            return texture;
        }
        
        protected abstract T bufferTextureData();

        protected abstract void freeResources();
    }

    record TextureData(ByteBuffer textureData, Vector2i pixelSize, Format format) {
    }

    protected AbstractTexture(Builder<? extends AbstractTexture> builder) {
        this.texture = glGenTextures();
        this.format = builder.format;
        this.type = builder.type;
        this.target = builder.target;

        bind(0);

        glTexParameteri(target.gl(), GL_TEXTURE_MIN_FILTER, builder.minFilter.gl());
        glTexParameteri(target.gl(), GL_TEXTURE_MAG_FILTER, builder.magFilter.gl());

        glTexParameteri(target.gl(), GL_TEXTURE_WRAP_S, builder.wrapping.gl());
        glTexParameteri(target.gl(), GL_TEXTURE_WRAP_T, builder.wrapping.gl());
        glTexParameteri(target.gl(), GL_TEXTURE_WRAP_R, builder.wrapping.gl());

        if (builder.autoSwizzleMask) {
            int[] swizzleMask = switch (builder.format) {
                case GRAY, DEPTH, STENCIL -> new int[] {GL_RED, GL_RED, GL_RED, GL_ONE};
                case ALPHA -> new int[] {GL_ONE, GL_ONE, GL_ALPHA, GL_RED};
                case GA -> new int[] {GL_RED, GL_RED, GL_RED, GL_GREEN};
                case RGB, RGBA, SRGB, SRGBA -> new int[]{GL_RED, GL_GREEN, GL_BLUE, GL_ALPHA};
                case NONE -> throw new IllegalStateException("No swizzle mask for colour format: " + builder.format.name());
            };

            glTexParameteriv(target.gl(), GL_TEXTURE_SWIZZLE_RGBA, swizzleMask);
        }
    }

    //TODO rework binding system; perhaps standard bind to 0, and only use units in specific cases like rendering
    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(target.gl(), texture);
    }

    public void unbind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(target.gl(), 0);
    }

    public int getID() {
        return texture;
    }

    public Format getFormat() {
        return format;
    }

    public Type getType() {
        return type;
    }

    public Target getTarget() {
        return target;
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
        if (!(o instanceof AbstractTexture that)) return false;
        return texture == that.texture && format == that.format && type == that.type && target == that.target;
    }

    @Override
    public int hashCode() {
        return Objects.hash(texture, format, type, target);
    }

}
