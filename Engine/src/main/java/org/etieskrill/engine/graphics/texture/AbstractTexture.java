package org.etieskrill.engine.graphics.texture;

import glm_.vec2.Vec2i;
import org.etieskrill.engine.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Objects;

import static org.etieskrill.engine.graphics.texture.Textures.NR_BITS_PER_COLOUR_CHANNEL;
import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.assimp.Assimp.aiTextureType_EMISSIVE;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL11C.GL_REPEAT;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13C.*;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL30C.GL_RG;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_SWIZZLE_RGBA;

public abstract class AbstractTexture implements Disposable {

    static final String DIRECTORY = "Engine/src/main/resources/textures/";

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

    public static abstract class Builder<T extends AbstractTexture> {
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

        public abstract T build();

        protected abstract void freeResources();
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
        glBindTexture(target.gl(), texture);
    }

    public void unbind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(target.gl(), 0);
    }

    public static void unbindAllTextures() {
        for (int i = 0; i < 32; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, 0); //i presume the target does not matter here
        }
    }

    //TODO create private wrapper class which opens this to classes such as FrameBuffer
    @Deprecated
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
        return texture == that.texture && wasAlreadyDisposed == that.wasAlreadyDisposed && format == that.format && type == that.type && target == that.target;
    }

    @Override
    public int hashCode() {
        return Objects.hash(texture, format, type, target, wasAlreadyDisposed);
    }

}
