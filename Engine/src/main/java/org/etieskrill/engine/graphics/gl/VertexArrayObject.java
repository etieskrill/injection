package org.etieskrill.engine.graphics.gl;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.etieskrill.engine.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import static java.lang.Integer.toHexString;
import static java.util.Objects.requireNonNullElse;
import static lombok.AccessLevel.NONE;
import static org.etieskrill.engine.graphics.gl.GLUtils.checkErrorThrowing;
import static org.etieskrill.engine.graphics.gl.GLUtils.clearError;
import static org.lwjgl.opengl.GL30C.*;

/**
 * @param <A> type of data accessor
 */
@Slf4j
@Getter
public class VertexArrayObject<A extends VertexArrayAccessor<?>> implements Disposable {

    public static final int MAX_VERTEX_ATTRIB_BINDINGS = 16;

    @Getter(NONE)
    private final int id;
    private final @NotNull BufferObject vertexBuffer;
    private final @Nullable BufferObject indexBuffer;

    @SuppressWarnings("unchecked")
    public static <A extends VertexArrayAccessor<?>> VertexArrayObjectBuilder<A> builder(A accessor) {
        return (VertexArrayObjectBuilder<A>) new VertexArrayObjectBuilder<>().accessor(accessor);
    }

    @Builder
    private VertexArrayObject(@Nullable Long vertexBufferByteSize, @Nullable BufferObject vertexBuffer, @Nullable BufferObject indexBuffer, A accessor) {
        clearError();

        if (accessor == null) {
            throw new IllegalArgumentException("Accessor cannot be null");
        }

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

    private void configureAttributeArrays(A accessor) {
        record FieldType(int componentType, int componentsNumber) {
        }
        int totalStrideBytes = accessor.getFields().stream()
                .map(field -> new FieldType(
                        parseComponentType(requireNonNullElse(field.getComponentType(), field.getType())),
                        parseMatrixRowNumber(field.getType()) * parseComponentNumber(field.getType())))
                .mapToInt(type -> type.componentsNumber * getByteSize(type.componentType))
                .sum();

        //TODO if standard attrib naming - use glGetAttribLocation instead
        int bindingIndex = 0;
        int currentStrideBytes = 0;

        String configLog = "";

        for (var field : accessor.getFields()) {
            int glComponentType = parseComponentType(requireNonNullElse(field.getComponentType(), field.getType()));

            final int numMatrixRows = parseMatrixRowNumber(field.getType());
            for (int matrixRow = 0; matrixRow < numMatrixRows; matrixRow++) {
                int componentNumber = parseComponentNumber(field.getType());

                glEnableVertexAttribArray(bindingIndex);
                if (glComponentType == GL_INT || glComponentType == GL_BYTE || glComponentType == GL_SHORT) {
                    glVertexAttribIPointer(bindingIndex, componentNumber, glComponentType, totalStrideBytes, currentStrideBytes);
                } else {
                    glVertexAttribPointer(bindingIndex, componentNumber, glComponentType, field.isNormalised(), totalStrideBytes, currentStrideBytes);
                    //TODO attrib divisors for instancing
                }

                configLog += "\tbinding index " + bindingIndex + " to " + field.getType().getSimpleName()
                        + " (transformed to " + componentNumber + "d-" + (field.getComponentType() != null ? field.getComponentType() : field.getType()).getSimpleName().toLowerCase() + (numMatrixRows > 1 ? ", matrix row " + (matrixRow + 1) + " of " + numMatrixRows : "") + ") "
                        + (field.isNormalised() ? "normalised " : "") + "with offset of " + currentStrideBytes + " bytes, and total stride of " + totalStrideBytes + " bytes\n";

                bindingIndex++;
                currentStrideBytes += componentNumber * getByteSize(glComponentType);
            }

            if (bindingIndex >= MAX_VERTEX_ATTRIB_BINDINGS) {
                throw new BufferCreationException("Too many vertex attribute bindings at field " + bindingIndex);
            }
        }

        configLog = configLog.substring(0, configLog.length() - 1); //remove last newline
        logger.debug("Configured vertex attribute pointers:\n{}", configLog);
    }

    private static int parseComponentNumber(Class<?> type) {
        if (anyMatch(type, Integer.class, Long.class, Boolean.class, Float.class, Double.class)) return 1;
        else if (anyMatch(type, Vector2i.class, Vector2f.class, Vector2d.class, Matrix2f.class, Matrix2d.class))
            return 2;
        else if (anyMatch(type, Vector3i.class, Vector3f.class, Vector3d.class)) return 3;
        else if (anyMatch(type, Matrix3f.class, Matrix3d.class, Matrix3x2f.class, Matrix3x2d.class)) return 3;
        else if (anyMatch(type, Vector4i.class, Vector4f.class, Vector4d.class)) return 4;
        else if (anyMatch(type, Matrix4f.class, Matrix4d.class, Matrix4x3f.class, Matrix4x3d.class)) return 4;
        else throw new IllegalStateException("Cannot determine component number for type: " + type);
    }

    private static int parseMatrixRowNumber(Class<?> type) {
        if (anyMatch(type, Matrix2f.class, Matrix2d.class, Matrix3x2f.class, Matrix3x2d.class)) return 2;
        else if (anyMatch(type, Matrix3f.class, Matrix3f.class, Matrix4x3f.class, Matrix4x3d.class)) return 3;
        else if (anyMatch(type, Matrix4f.class, Matrix4d.class)) return 4;
        else return 1; //Unexpected types throw above already, so no further types needed
    }

    private static int parseComponentType(Class<?> object) {
        if (anyMatch(object, Integer.class, Boolean.class, Vector2i.class, Vector3i.class, Vector4i.class))
            return GL_INT;
        else if (anyMatch(object, Float.class, Vector2f.class, Vector3f.class, Vector4f.class)) return GL_FLOAT;
        else if (anyMatch(object, Matrix2f.class, Matrix3f.class, Matrix3x2f.class, Matrix4f.class, Matrix4x3f.class))
            return GL_FLOAT;
        else if (anyMatch(object, Double.class, Vector2d.class, Vector3d.class, Vector4d.class)) return GL_DOUBLE;
        else if (anyMatch(object, Matrix2d.class, Matrix3d.class, Matrix3x2d.class, Matrix4d.class, Matrix4x3d.class))
            return GL_DOUBLE;
        else if (anyMatch(object, Byte.class)) return GL_BYTE;
        else if (anyMatch(object, Short.class)) return GL_SHORT;
        else throw new IllegalStateException("Cannot determine component type for type: " + object);
    }

    private static boolean anyMatch(Class<?> clazz, Class<?>... matches) {
        for (Class<?> match : matches) {
            if (clazz.isAssignableFrom(match)) return true;
        }
        return false;
    }

    private static int getByteSize(int glDataType) {
        return switch (glDataType) {
            case GL_INT -> Integer.BYTES;
            case GL_FLOAT -> Float.BYTES;
            case GL_DOUBLE -> Double.BYTES;
            case GL_BYTE -> Byte.BYTES;
            case GL_SHORT -> Short.BYTES;
            default ->
                    throw new IllegalStateException("Cannot determine byte size for gl data type: 0x" + toHexString(glDataType));
        };
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
