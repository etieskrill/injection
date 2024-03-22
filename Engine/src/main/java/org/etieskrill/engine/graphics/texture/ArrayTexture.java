package org.etieskrill.engine.graphics.texture;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2ic;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static org.etieskrill.engine.graphics.texture.AbstractTexture.Target.ARRAY;
import static org.etieskrill.engine.graphics.texture.Textures.NR_BITS_PER_COLOUR_CHANNEL;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL12C.glTexImage3D;

public class ArrayTexture extends AbstractTexture {

    private static final Logger logger = LoggerFactory.getLogger(ArrayTexture.class);

    private final Vector2ic pixelSize;
    private final Integer length;

    public static final class BufferBuilder extends Builder<ArrayTexture> {

        private final Integer length; //TODO increase length (and expand buffer) with #addTexture calls
        private final ByteBuffer buffer;

        private final int numBytesPerTexture;

        public BufferBuilder(@NotNull Vector2ic pixelSize, @NotNull Integer length, @NotNull Format format) {
            this.pixelSize = pixelSize;
            this.length = length;
            this.format = format;

            this.buffer = BufferUtils.createByteBuffer(pixelSize.x() * pixelSize.y() * length * format.getChannels());
            this.numBytesPerTexture = pixelSize.x() * pixelSize.y() * format.getChannels();
        }

        public BufferBuilder addTexture(@Nullable ByteBuffer data) {
            if (data == null) {
                buffer.position(buffer.position() + numBytesPerTexture);
                return this;
            }
            if (data.remaining() != numBytesPerTexture) //TODO pad automatically if too small instead? specify which sides to pad explicitly
                throw new IllegalArgumentException("Texture buffer does not contain correct number of bytes for " +
                        pixelSize.x() + "x" + pixelSize.y() + " " + format.getChannels() + "-channel texture: expected " +
                        numBytesPerTexture + " but got " + data.remaining());
            if (buffer.position() + data.remaining() > buffer.capacity())
                throw new IllegalArgumentException("Texture buffer contains too much data: " + data.remaining() +
                        " does not fit in " + (buffer.capacity() - buffer.position()) + " bytes");

            buffer.put(data); //TODO i think it's this operation which causes segfaults sometimes, one of the buffers is probably not allocated correctly
            logger.trace("Added texture to array, now contains {} of {} bytes", buffer.position(), buffer.capacity());
            return this;
        }

        @Override
        protected ArrayTexture bufferTextureData() {
            logger.debug("Loading {}x{} {}-bit {} array texture containing {} elements from {}",
                    pixelSize.x(), pixelSize.y(), NR_BITS_PER_COLOUR_CHANNEL * format.getChannels(),
                    type.name().toLowerCase(), length, getClass().getSimpleName());

            ArrayTexture texture = new ArrayTexture(this);
            texture.bind(0);

            glTexImage3D(target.gl(), 0, format.toGlInternalFormat(), pixelSize.x(), pixelSize.y(), length,
                    0, format.toGLFormat(), GL_UNSIGNED_BYTE, buffer.rewind());

            return texture;
        }

        @Override
        protected void freeResources() {
            //TODO free buffer
        }
    }

    protected ArrayTexture(BufferBuilder builder) {
        super(builder.setTarget(ARRAY));

        this.pixelSize = builder.pixelSize;
        this.length = builder.length;
    }

    public Vector2ic getPixelSize() {
        return pixelSize;
    }

    public Integer getLength() {
        return length;
    }

}
