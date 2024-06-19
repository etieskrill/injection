package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.etieskrill.engine.graphics.gl.BufferObject.AccessType.DRAW;
import static org.etieskrill.engine.graphics.gl.BufferObject.Frequency.STATIC;
import static org.etieskrill.engine.graphics.gl.BufferObject.Target.ARRAY;
import static org.etieskrill.engine.graphics.gl.GLUtils.checkErrorThrowing;
import static org.etieskrill.engine.graphics.gl.GLUtils.clearError;
import static org.etieskrill.engine.util.ClassUtils.getSimpleName;
import static org.lwjgl.opengl.GL15C.*;

public class BufferObject implements Disposable {

    private final Target target;
    private final int id;

    public static Builder create(long byteSize) {
        return new Builder(byteSize);
    }

    public static Builder create(Buffer buffer) {
        return new Builder(buffer);
    }

    public static class Builder {
        private final @Nullable Long byteSize;
        private final @Nullable Buffer buffer;

        private Target target = ARRAY;
        private Frequency frequency = STATIC;
        private AccessType accessType = DRAW;

        public Builder(long byteSize) {
            this.byteSize = byteSize;
            this.buffer = null;
        }

        public Builder(Buffer buffer) {
            this.byteSize = null;
            this.buffer = buffer;
        }

        public Builder target(@NotNull Target target) {
            this.target = target;
            return this;
        }

        public Builder frequency(@NotNull Frequency frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder accessType(@NotNull AccessType accessType) {
            this.accessType = accessType;
            return this;
        }

        public BufferObject build() {
            return new BufferObject(byteSize, buffer, target, frequency, accessType);
        }
    }

    public BufferObject(Long byteSize, Buffer buffer, Target target, Frequency frequency, AccessType accessType) {
        clearError();

        this.target = target;
        this.id = glGenBuffers();
        bind();
        if (buffer != null) {
            buffer.rewind();
            switch (buffer) {
                case ByteBuffer byteBuffer -> glBufferData(target.gl(), byteBuffer, toGLUsage(frequency, accessType));
                case IntBuffer intBuffer -> glBufferData(target.gl(), intBuffer, toGLUsage(frequency, accessType));
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

    public void setData(ByteBuffer data) {
        setData(0L, data);
    }

    public void setData(long offset, ByteBuffer data) {
        data.rewind();
        bind();
        glBufferSubData(target.gl(), offset, data);
    }

    public Target getTarget() {
        return target;
    }

    @Override
    public void dispose() {
        glDeleteBuffers(id);
    }

}
