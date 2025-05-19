package org.etieskrill.engine.graphics.gl;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.etieskrill.engine.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collection;

import static lombok.AccessLevel.NONE;
import static org.etieskrill.engine.graphics.gl.BufferObject.Target.ELEMENT_ARRAY;
import static org.etieskrill.engine.graphics.gl.GLUtils.checkErrorThrowing;
import static org.etieskrill.engine.graphics.gl.GLUtils.clearError;
import static org.lwjgl.opengl.GL30C.*;

/**
 * @param <T> type of vertex data
 */
@Getter
public class VertexArrayObject<T> implements Disposable {

    public static final int MAX_VERTEX_ATTRIB_BINDINGS = 16;

    private final @Getter(NONE) int id;
    private final @NotNull BufferObject vertexBuffer;
    private final @Nullable BufferObject indexBuffer;

    private final @Delegate VertexArrayAccessor<T> accessor;

    private static final Logger logger = LoggerFactory.getLogger(VertexArrayObject.class);

    @SuppressWarnings("unchecked")
    public static <T> VertexArrayObjectBuilder<T> builder(VertexArrayAccessor<T> accessor) {
        return (VertexArrayObjectBuilder<T>) new VertexArrayObjectBuilder<>().accessor((VertexArrayAccessor<Object>) accessor);
    }

    //TODO add dummy accessor factory method - and probs class with UnsupportedOperationException

    @Builder
    private VertexArrayObject(@Nullable Long numVertexElements,
                              @Nullable Collection<T> vertexElements,
                              @Nullable BufferObject vertexBuffer,
                              @Nullable Integer numIndices,
                              @Nullable Collection<Integer> indices,
                              @Nullable BufferObject indexBuffer,
                              VertexArrayAccessor<T> accessor,
                              @Nullable BufferObject.Frequency frequency,
                              @Nullable BufferObject.AccessType accessType
    ) {
        clearError();

        if (accessor == null) {
            throw new IllegalArgumentException("Accessor cannot be null");
        }
        this.accessor = accessor;

        this.id = glGenVertexArrays();
        bind();

        if (vertexBuffer != null) {
            if (vertexBuffer.getTarget() != BufferObject.Target.ARRAY) {
                throw new IllegalArgumentException("Vertex buffer must be an array buffer");
            }
            this.vertexBuffer = vertexBuffer;
            this.vertexBuffer.bind();
        } else if (vertexElements != null) {
            this.vertexBuffer = BufferObject
                    .create(accessor.getElementByteSize(), vertexElements.size())
                    .frequency(frequency)
                    .accessType(accessType)
                    .build();
            this.vertexBuffer.bind();
            setVertices(vertexElements);
        } else if (numVertexElements != null) {
            this.vertexBuffer = BufferObject
                    .create(accessor.getElementByteSize(), Math.toIntExact(numVertexElements))
                    .frequency(frequency)
                    .accessType(accessType)
                    .build();
            this.vertexBuffer.bind();
        } else {
            throw new BufferCreationException("Vertex buffer size, data or buffer object must be set");
        }

        configureAttributeArrays(accessor);

        if (indexBuffer != null) {
            if (indexBuffer.getTarget() != ELEMENT_ARRAY) {
                throw new IllegalArgumentException("Index buffer must be an element array buffer");
            }
            this.indexBuffer = indexBuffer;
            this.indexBuffer.bind();
        } else if (indices != null) {
            this.indexBuffer = BufferObject
                    .create(Integer.BYTES, indices.size()).target(ELEMENT_ARRAY)
                    .frequency(frequency)
                    .accessType(accessType)
                    .build();
            this.indexBuffer.bind();
            setIndices(indices);
        } else if (numIndices != null) {
            this.indexBuffer = BufferObject
                    .create(Integer.BYTES, numIndices).target(ELEMENT_ARRAY)
                    .frequency(frequency)
                    .accessType(accessType)
                    .build();
            this.indexBuffer.bind();
        } else {
            this.indexBuffer = null;
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

        StringBuilder configLog = new StringBuilder();

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

                configLog.append("\tbinding index ").append(bindingIndex).append(" to ")
                        .append(field.getType().getSimpleName()).append(" (transformed to ").append(numComponents)
                        .append("d-").append(componentType.getSimpleName().toLowerCase())
                        .append(field.getNumMatrixRows() > 1 ? ", matrix row " + (matrixRow + 1) + " of " + field.getNumMatrixRows() : "")
                        .append(") ").append(field.isNormalised() ? "normalised " : "").append("with offset of ")
                        .append(currentStrideBytes).append(" bytes, and total stride of ").append(totalStrideBytes)
                        .append(" bytes\n");

                bindingIndex++;
                currentStrideBytes += numComponents * field.getComponentByteSize();
            }

            if (bindingIndex >= MAX_VERTEX_ATTRIB_BINDINGS) {
                throw new BufferCreationException("Too many vertex attribute bindings at field " + bindingIndex);
            }
        }

        configLog.deleteCharAt(configLog.length() - 1); //Remove last newline
        logger.debug("Configured vertex attribute pointers:\n{}", configLog);
    }

    public void setVertices(Collection<T> vertices) {
        ByteBuffer buffer = vertexBuffer.getBuffer()
                .rewind()
                .limit(accessor.getElementByteSize() * vertices.size());
        int position = 0;
        for (T value : vertices) {
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

    public void setIndices(Collection<Integer> indices) {
        if (indexBuffer == null) {
            throw new IllegalStateException("Vertex array object is not indexed");
        }

        ByteBuffer indexBuffer = this.indexBuffer.getBuffer().rewind();
        if (indices.size() > indexBuffer.capacity() * Integer.BYTES) {
            throw new IllegalArgumentException("Too many indices for index buffer object");
        }

        indices.forEach(indexBuffer::putInt);
        this.indexBuffer.setData(indexBuffer);
    }

    public void bind() {
        glBindVertexArray(id);
    }

    public static void unbind() {
        glBindVertexArray(0);
    }

    public boolean isIndexed() {
        return indexBuffer != null;
    }

    @Override
    public void dispose() {
        glDeleteVertexArrays(id);
        vertexBuffer.dispose();
        if (indexBuffer != null) indexBuffer.dispose();
    }

}
