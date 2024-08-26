package org.etieskrill.engine.graphics.gl;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.etieskrill.engine.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Collection;

import static lombok.AccessLevel.NONE;
import static org.etieskrill.engine.graphics.gl.GLUtils.checkErrorThrowing;
import static org.etieskrill.engine.graphics.gl.GLUtils.clearError;
import static org.lwjgl.opengl.GL30C.*;

/**
 * @param <T> type of vertex data
 */
@Slf4j
@Getter
public class VertexArrayObject<T> implements Disposable {

    public static final int MAX_VERTEX_ATTRIB_BINDINGS = 16;

    private final @Getter(NONE) int id;
    private final @NotNull BufferObject vertexBuffer;
    private final @Nullable BufferObject indexBuffer;

    private final @Delegate VertexArrayAccessor<T> accessor;

    @SuppressWarnings("unchecked")
    public static <T> VertexArrayObjectBuilder<T> builder(VertexArrayAccessor<T> accessor) {
        return (VertexArrayObjectBuilder<T>) new VertexArrayObjectBuilder<>().accessor((VertexArrayAccessor<Object>) accessor);
    }

    @Builder
    private VertexArrayObject(@Nullable Long vertexBufferByteSize, @Nullable BufferObject vertexBuffer, @Nullable BufferObject indexBuffer, VertexArrayAccessor<T> accessor) {
        clearError();

        if (accessor == null) {
            throw new IllegalArgumentException("Accessor cannot be null");
        }
        this.accessor = accessor;

        this.id = glGenVertexArrays();
        bind();

        if (vertexBufferByteSize != null) {
            this.vertexBuffer = BufferObject.create(vertexBufferByteSize).build();
        } else if (vertexBuffer != null) {
            if (vertexBuffer.getTarget() != BufferObject.Target.ARRAY) {
                throw new IllegalArgumentException("Vertex buffer must be an array buffer");
            }
            this.vertexBuffer = vertexBuffer;
            vertexBuffer.bind();
        } else {
            throw new BufferCreationException("Vertex buffer size or buffer data must be set");
        }

        configureAttributeArrays(accessor);

        this.indexBuffer = indexBuffer;
        if (indexBuffer != null) {
            if (indexBuffer.getTarget() != BufferObject.Target.ELEMENT_ARRAY) {
                throw new IllegalArgumentException("Index buffer must be an element array buffer");
            }
            indexBuffer.bind();
        }

        checkErrorThrowing("Failed to create vertex array object", BufferCreationException::new);
    }

    private void configureAttributeArrays(VertexArrayAccessor<T> accessor) {
        int totalStrideBytes = accessor.getFields().stream()
                .mapToInt(VertexArrayAccessor.FieldAccessor::getFieldByteSize)
                .sum();

        //TODO if standard attrib naming - use glGetAttribLocation instead
        int bindingIndex = 0;
        int currentStrideBytes = 0;

        String configLog = "";

        for (var field : accessor.getFields()) {
            for (int matrixRow = 0; matrixRow < field.getNumMatrixRows(); matrixRow++) {
                int numComponents = field.getNumComponents();

                glEnableVertexAttribArray(bindingIndex);
                var componentType = field.getComponentType();
                if (componentType == Integer.class || componentType == Byte.class || componentType == Short.class) {
                    glVertexAttribIPointer(bindingIndex, numComponents, field.getGlComponentType(), totalStrideBytes, currentStrideBytes);
                } else {
                    glVertexAttribPointer(bindingIndex, numComponents, field.getGlComponentType(), field.isNormalised(), totalStrideBytes, currentStrideBytes);
                    //TODO attrib divisors for instancing
                }

                configLog += "\tbinding index " + bindingIndex + " to " + field.getType().getSimpleName()
                        + " (transformed to " + numComponents + "d-" + componentType.getSimpleName().toLowerCase()
                        + (field.getNumMatrixRows() > 1 ? ", matrix row " + (matrixRow + 1) + " of " + field.getNumMatrixRows() : "") + ") "
                        + (field.isNormalised() ? "normalised " : "")
                        + "with offset of " + currentStrideBytes + " bytes, and total stride of " + totalStrideBytes + " bytes\n";

                bindingIndex++;
                currentStrideBytes += numComponents * field.getComponentByteSize();
            }

            if (bindingIndex >= MAX_VERTEX_ATTRIB_BINDINGS) {
                throw new BufferCreationException("Too many vertex attribute bindings at field " + bindingIndex);
            }
        }

        configLog = configLog.substring(0, configLog.length() - 1); //remove last newline
        logger.debug("Configured vertex attribute pointers:\n{}", configLog);
    }

    public void setAll(Collection<T> values) {
        ByteBuffer buffer = vertexBuffer.getBuffer()
                .rewind()
                .limit(accessor.getElementByteSize() * values.size());
        int position = 0;
        for (T value : values) {
            for (var field : accessor.getFields()) {
                buffer.position(position);
                position += field.getFieldByteSize(); //FIXME maybe too much handholding?
                field.getAccessor().accept(value, buffer);
            }
        }

        if (position != buffer.limit()) {
            throw new IllegalStateException("Vertex buffer position does not align with data byte length");
        }

        vertexBuffer.setData(buffer);
    }

    public void bind() {
        glBindVertexArray(id);
    }

    public static void unbind() {
        glBindVertexArray(0);
    }

    public boolean isIndexed() {
        return indexBuffer == null;
    }

    @Override
    public void dispose() {
        glDeleteVertexArrays(id);
        vertexBuffer.dispose();
        if (indexBuffer != null) indexBuffer.dispose();
    }

}
