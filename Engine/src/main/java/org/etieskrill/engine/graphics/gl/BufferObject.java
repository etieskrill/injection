package org.etieskrill.engine.graphics.gl;

import lombok.Getter;
import org.etieskrill.engine.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static java.util.Objects.requireNonNullElse;
import static org.etieskrill.engine.graphics.gl.BufferObject.AccessType.DRAW;
import static org.etieskrill.engine.graphics.gl.BufferObject.Frequency.STATIC;
import static org.etieskrill.engine.graphics.gl.BufferObject.Target.ARRAY;
import static org.etieskrill.engine.graphics.gl.GLUtils.checkErrorThrowing;
import static org.etieskrill.engine.graphics.gl.GLUtils.clearError;
import static org.etieskrill.engine.util.ClassUtils.getSimpleName;
import static org.lwjgl.opengl.GL15C.*;

public class BufferObject implements Disposable {

    @Getter
    private final Target target;
    private final int id;

    private final @Getter int size;

    private ByteBuffer buffer;

    //TODO add vao slot to ensure in-use (bound) buffers are not accidentally used elsewhere

    public static Builder create(int elementByteSize, int numElements) {
        return new Builder((long) elementByteSize * numElements, null, numElements);
    }

    public static Builder create(Buffer buffer) {
        return new Builder(null, buffer, buffer.capacity());
    }

    public static class Builder {
        private final @Nullable Long byteSize;
        private final @Nullable Buffer buffer;
        private final int numElements;

        private Target target = ARRAY;
        private Frequency frequency = STATIC;
        private AccessType accessType = DRAW;

        private Builder(@Nullable Long byteSize, @Nullable Buffer buffer, int numElements) {
            this.byteSize = byteSize;
            this.buffer = buffer;
            this.numElements = numElements;
        }

        public Builder target(@NotNull Target target) {
            this.target = target;
            return this;
        }

        public Builder frequency(@Nullable Frequency frequency) {
            this.frequency = requireNonNullElse(frequency, STATIC);
            return this;
        }

        public Builder accessType(@Nullable AccessType accessType) {
            this.accessType = requireNonNullElse(accessType, DRAW);
            return this;
        }

        public BufferObject build() {
            return new BufferObject(byteSize, buffer, numElements, target, frequency, accessType);
        }
    }

    private BufferObject(Long byteSize, Buffer buffer, int numElements, Target target, Frequency frequency, AccessType accessType) {
        clearError();

        this.size = numElements;
        this.target = target;
        this.id = glGenBuffers();
        bind();
        if (buffer != null) {
            buffer.rewind();
            switch (buffer) {
                case ByteBuffer byteBuffer -> glBufferData(target.gl(), byteBuffer, toGLUsage(frequency, accessType));
                case IntBuffer intBuffer -> glBufferData(target.gl(), intBuffer, toGLUsage(frequency, accessType));
                case FloatBuffer floatBuffer ->
                        glBufferData(target.gl(), floatBuffer, toGLUsage(frequency, accessType));
                default -> throw new IllegalArgumentException("Unrecognised buffer type: " + getSimpleName(buffer));
            }
        } else if (byteSize != null) {
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

    /**
     * Returns a {@link ByteBuffer} sized to this {@link BufferObject}. On the first call to this method (or
     * {@link BufferObject#getData()}), a new buffer is created, after which point the same object is always returned.
     *
     * @return a buffer sized to this buffer object
     */
    public ByteBuffer getBuffer() {
        if (buffer == null) {
            bind();
            int size = glGetBufferParameteri(target.gl(), GL_BUFFER_SIZE);
            buffer = BufferUtils.createByteBuffer(size);
        }
        return buffer;
    }

    /**
     * Returns a {@link ByteBuffer} containing the data in the buffer object. On the first call to this method (or
     * {@link BufferObject#getBuffer()}), a new buffer sized to this buffer object is created, after which point this
     * same object is always returned.
     * <p>
     * Writing to the buffer does not affect this {@link BufferObject}. Instead, the usual
     * {@link BufferObject#setData(ByteBuffer)} can be used to write back any changes to the object.
     *
     * @return a buffer containing the buffer object's data
     */
    public ByteBuffer getData() {
        bind();
        glGetBufferSubData(target.gl(), 0, getBuffer().rewind());
        return buffer;
    }

    public void setData(ByteBuffer data) {
        setData(0L, data);
    }

    public void setData(long offset, ByteBuffer data) {
        data.rewind();
        bind();
        glBufferSubData(target.gl(), offset, data);
    }

    @Override
    public void dispose() {
        glDeleteBuffers(id);
    }

}
