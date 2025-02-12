package org.etieskrill.engine.graphics.texture;

import io.github.etieskrill.injection.extension.shaderreflection.TextureCubeMapArray;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2ic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static org.etieskrill.engine.graphics.texture.Textures.NR_BITS_PER_COLOUR_CHANNEL;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL12C.glTexImage3D;
import static org.lwjgl.opengl.GL32C.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL32C.glFramebufferTexture;

public class CubeMapArrayTexture extends AbstractTexture implements FrameBufferAttachment, TextureCubeMapArray {

    private static final Logger logger = LoggerFactory.getLogger(CubeMapArrayTexture.class);

    private final Vector2ic pixelSize;
    private final Integer length;

    public static final class BlankBuilder extends Builder<CubeMapArrayTexture> {
        private final int length;

        public BlankBuilder(@NotNull Vector2ic pixelSize, int length) {
            this.pixelSize = pixelSize;
            this.length = length;
        }

        @Override
        protected CubeMapArrayTexture bufferTextureData() {
            logger.debug("Loading {}x{} {}-bit {} {} cubemap array texture containing {} elements from {}",
                    pixelSize.x(), pixelSize.y(), NR_BITS_PER_COLOUR_CHANNEL * format.getChannels(),
                    format.name().toLowerCase(), type.name().toLowerCase(), length, getClass().getSimpleName());

            CubeMapArrayTexture texture = new CubeMapArrayTexture(this);
            texture.bind(0);

            glTexImage3D(target.gl(), 0, format.toGlInternalFormat(), pixelSize.x(), pixelSize.y(),
                    CubeMapTexture.NUM_SIDES * length, 0, format.toGLFormat(), GL_UNSIGNED_BYTE,
                    (ByteBuffer) null);

            return texture;
        }

        @Override
        protected void freeResources() {
        }
    }

    protected CubeMapArrayTexture(BlankBuilder builder) {
        super(builder.setTarget(Target.CUBEMAP_ARRAY));

        this.pixelSize = builder.pixelSize;
        this.length = builder.length;
    }

    @Override
    public Vector2ic getSize() {
        return pixelSize;
    }

    @Override
    public void attach(BufferAttachmentType type) {
        glFramebufferTexture(GL_FRAMEBUFFER, type.toGLAttachment(), getID(), 0);
    }

    public Integer getLength() {
        return length;
    }

}
