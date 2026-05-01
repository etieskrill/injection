package org.etieskrill.engine.graphics.gl;

import io.github.etieskrill.injection.extension.shader.BufferAccessor;
import lombok.Getter;
import org.etieskrill.engine.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collection;

import static java.util.Objects.requireNonNullElse;
import static org.etieskrill.engine.graphics.gl.BufferObject.AccessType.DRAW;
import static org.etieskrill.engine.graphics.gl.BufferObject.Frequency.STATIC;
import static org.etieskrill.engine.graphics.gl.BufferObject.Target.ARRAY;
import static org.etieskrill.engine.graphics.gl.GLUtils.checkErrorThrowing;
import static org.etieskrill.engine.graphics.gl.GLUtils.clearError;
import static org.etieskrill.engine.util.ClassUtils.getSimpleName;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL30C.GL_R8I;
import static org.lwjgl.opengl.GL43C.glClearBufferSubData;

public class BufferObject<T> implements io.github.etieskrill.injection.extension.shader.Buffer<T>, Disposable {

    @Getter
    private final Target target;
    private final int id;

    private final @Getter int numElements;
    private final @Getter long byteSize;

    private final BufferAccessor<T> accessor;
    private ByteBuffer buffer;

    private static final Logger logger = LoggerFactory.getLogger(BufferObject.class);

    //TODO add vao slot to ensure in-use (bound) buffers are not accidentally used elsewhere

    public static <T> Builder<T> create(BufferAccessor<T> accessor, int numElements) {
        return new Builder<>(accessor, (long) accessor.getElementByteSize() * numElements, null, numElements);
    }

    public static <T> Builder<T> create(BufferAccessor<T> accessor, Buffer buffer) {
        return new Builder<>(accessor, null, buffer, buffer.capacity());
    }

    public static class Builder<T> {
        private final BufferAccessor<T> accessor;
        private final @Nullable Long byteSize;
        private final @Nullable Buffer buffer;
        private final int numElements;

        private Target target = ARRAY;
        private Frequency frequency = STATIC;
        private AccessType accessType = DRAW;

        private Builder(@NotNull BufferAccessor<T> accessor, @Nullable Long byteSize, @Nullable Buffer buffer, int numElements) {
            this.accessor = accessor;
            this.byteSize = byteSize;
            this.buffer = buffer;
            this.numElements = numElements;
        }

        public Builder<T> target(@NotNull Target target) {
            this.target = target;
            return this;
        }

        public Builder<T> frequency(@Nullable Frequency frequency) {
            this.frequency = requireNonNullElse(frequency, STATIC);
            return this;
        }

        public Builder<T> accessType(@Nullable AccessType accessType) {
            this.accessType = requireNonNullElse(accessType, DRAW);
            return this;
        }

        public BufferObject<T> build() {
            return new BufferObject<>(accessor, byteSize, buffer, numElements, target, frequency, accessType);
        }
    }

    protected BufferObject(BufferAccessor<T> accessor, Long byteSize, Buffer buffer, int numElements, Target target, Frequency frequency, AccessType accessType) {
        clearError();

        this.accessor = accessor;
        this.numElements = numElements;
        this.target = target;

        this.id = glGenBuffers();
        bind();

        if (buffer != null) {
            buffer.rewind();
            switch (buffer) {
                case ByteBuffer byteBuffer -> {
                    this.byteSize = (long) Byte.BYTES * numElements;
                    glBufferData(target.gl(), byteBuffer, toGLUsage(frequency, accessType));
                }
                case IntBuffer intBuffer -> {
                    this.byteSize = (long) Integer.BYTES * numElements;
                    glBufferData(target.gl(), intBuffer, toGLUsage(frequency, accessType));
                }
                case FloatBuffer floatBuffer -> {
                    this.byteSize = (long) Float.BYTES * numElements;
                    glBufferData(target.gl(), floatBuffer, toGLUsage(frequency, accessType));
                }
                default -> throw new IllegalArgumentException("Unrecognised buffer type: " + getSimpleName(buffer));
            }
        } else if (byteSize != null) {
            this.byteSize = byteSize;
            glBufferData(target.gl(), byteSize, toGLUsage(frequency, accessType));
        } else {
            throw new IllegalArgumentException("Either buffer size or buffer data must be set");
        }

        checkErrorThrowing("Failed to create buffer object of type %s with size of %d bytes"
                .formatted(target.name(), byteSize), BufferCreationException::new);
    }

    public enum Target {
        ARRAY(GL_ARRAY_BUFFER),
        ELEMENT_ARRAY(GL_ELEMENT_ARRAY_BUFFER);

        private final int glTarget;

        Target(int glTarget) {
            this.glTarget = glTarget;
        }

        public int gl() {
            return glTarget;
        }
    }

    public enum Frequency {STATIC, STREAM, DYNAMIC}

    public enum AccessType {DRAW, READ, COPY}

    private static int toGLUsage(Frequency frequency, AccessType accessType) {
        return switch (frequency) {
            case STATIC -> switch (accessType) {
                case DRAW -> GL_STATIC_DRAW;
                case READ -> GL_STATIC_READ;
                case COPY -> GL_STATIC_COPY;
            };
            case STREAM -> switch (accessType) {
                case DRAW -> GL_STREAM_DRAW;
                case READ -> GL_STREAM_READ;
                case COPY -> GL_STREAM_COPY;
            };
            case DYNAMIC -> switch (accessType) {
                case DRAW -> GL_DYNAMIC_DRAW;
                case READ -> GL_DYNAMIC_READ;
                case COPY -> GL_DYNAMIC_COPY;
            };
        };
    }

    public void bind() {
        glBindBuffer(target.gl(), id);
    }

    public void unbind() {
        glBindBuffer(target.gl(), 0);
    }

    @Override
    public @NotNull BufferAccessor<T> getAccessor() {
        return accessor;
    }

    /**
     * Returns a {@link ByteBuffer} sized to this {@link BufferObject}. On the first call to this method (or
     * {@link BufferObject#getData()}), a new buffer is created, after which point the same object is always returned.
     *
     * @return a buffer sized to this buffer object
     */
    public @NotNull ByteBuffer getBuffer() {
        if (buffer == null) {
            bind();
            buffer = BufferUtils.createByteBuffer((int) byteSize);
        }
        return buffer;
    }

    /**
     * Returns a {@link ByteBuffer} containing the data in the buffer object. On the first call to this method (or
     * {@link BufferObject#getBuffer()}), a new buffer sized to this buffer object is created, after which point this
     * same object is always returned.
     * <p>
     * Writing to the buffer does not affect this {@link BufferObject}. Instead,
     * {@link BufferObject#setData(ByteBuffer)} can be used to write to the object.
     *
     * @return a buffer containing the buffer object's data
     */
    public ByteBuffer getData() {
        bind();
        glGetBufferSubData(target.gl(), 0, getBuffer().clear());
        return buffer; //TODO asReadOnlyBuffer?
    }

    @Override
    public void setData(@NotNull Collection<? extends T> elements) {
        getAccessor().map(elements, this);
    }

    /**
     * Sets the buffer's data to the values provided in {@code data}.
     *
     * @param data the data to set in the buffer
     */
    public void setData(@NotNull ByteBuffer data) {
        setData(data, false);
    }

    /**
     * Sets the buffer's data to the values provided in {@code data}. The buffer is cleared beforehand if the
     * {@code clear} flag is set.
     *
     * @param data  the data to set in the buffer
     * @param clear if {@code true}, buffer is cleared to all zeroes before data is set
     */
    public void setData(ByteBuffer data, boolean clear) {
        setData(0L, data, clear);
    }

    /**
     * Sets the buffer's data at {@code offset} to the values provided in {@code data}. The buffer is cleared beforehand
     * if the {@code clear} flag is set.
     *
     * @param offset offset to the start of the buffer
     * @param data   the data to set in the buffer
     * @param clear  if {@code true}, buffer is cleared to all zeroes before data is set
     */
    public void setData(long offset, ByteBuffer data, boolean clear) {
        if (data.limit() - data.position() > byteSize - offset) //TODO is possible data still set in backend?
            logger.warn("Data buffer size exceeds capacity of BufferObject");

        data.rewind();
        bind();
        if (clear) glClearBufferSubData(target.gl(), GL_R8I, 0, byteSize, GL_RED, GL_BYTE, (ByteBuffer) null);
        glBufferSubData(target.gl(), offset, data);
    }

    @Override
    public void dispose() {
        glDeleteBuffers(id);
    }

}
