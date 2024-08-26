package org.etieskrill.engine.graphics.gl;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNullElse;
import static lombok.AccessLevel.PACKAGE;
import static org.lwjgl.opengl.GL11C.*;

/**
 * @param <T> type of data accessed
 */
public abstract class VertexArrayAccessor<T> {

    protected final @Getter(PACKAGE) List<FieldAccessor<?>> fields;
    private final @Getter(PACKAGE) int elementByteSize;

    @Getter
    protected final class FieldAccessor<F> {
        private final Class<F> type;
        private final Class<? extends Number> componentType;
        private final FieldMapper<T> accessor;
        private final boolean normalised;

        private final int numComponents;
        private final int numMatrixRows;
        private final int glComponentType;
        private final int componentByteSize;
        private final int fieldByteSize;

        private FieldAccessor(Class<F> type, @Nullable Class<? extends Number> componentType, FieldMapper<T> accessor, boolean normalised) {
            this.type = type;
            this.componentType = requireNonNullElse(componentType, parseComponentType(type));
            this.accessor = accessor;
            this.normalised = normalised;

            this.numComponents = parseComponentNumber(type);
            this.numMatrixRows = parseMatrixRowNumber(type);
            this.glComponentType = toGlComponentType(this.componentType);
            this.componentByteSize = getComponentByteSize(this.componentType);
            this.fieldByteSize = numComponents * numMatrixRows * componentByteSize;
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

        private static Class<? extends Number> parseComponentType(Class<?> object) {
            if (anyMatch(object, Integer.class, Boolean.class, Vector2i.class, Vector3i.class, Vector4i.class))
                return Integer.class;
            else if (anyMatch(object, Float.class, Vector2f.class, Vector3f.class, Vector4f.class)) return Float.class;
            else if (anyMatch(object, Matrix2f.class, Matrix3f.class, Matrix3x2f.class, Matrix4f.class, Matrix4x3f.class))
                return Float.class;
            else if (anyMatch(object, Double.class, Vector2d.class, Vector3d.class, Vector4d.class))
                return Double.class;
            else if (anyMatch(object, Matrix2d.class, Matrix3d.class, Matrix3x2d.class, Matrix4d.class, Matrix4x3d.class))
                return Double.class;
            else if (anyMatch(object, Byte.class)) return Byte.class;
            else if (anyMatch(object, Short.class)) return Short.class;
            else throw new IllegalStateException("Cannot determine component type for type: " + object);
        }

        private static boolean anyMatch(Class<?> clazz, Class<?>... matches) {
            for (Class<?> match : matches) {
                if (clazz.isAssignableFrom(match)) return true;
            }
            return false;
        }

        private static int toGlComponentType(Class<? extends Number> type) {
            if (type.isAssignableFrom(Integer.class)) return GL_INT;
            else if (type.isAssignableFrom(Float.class)) return GL_FLOAT;
            else if (type.isAssignableFrom(Double.class)) return GL_DOUBLE;
            else if (type.isAssignableFrom(Byte.class)) return GL_BYTE;
            else if (type.isAssignableFrom(Short.class)) return GL_SHORT;
            else throw new IllegalStateException("Cannot determine gl component type for type: " + type);
        }

        private static int getComponentByteSize(Class<? extends Number> type) {
            if (type.isAssignableFrom(Integer.class)) return Integer.BYTES;
            else if (type.isAssignableFrom(Float.class)) return Float.BYTES;
            else if (type.isAssignableFrom(Double.class)) return Double.BYTES;
            else if (type.isAssignableFrom(Byte.class)) return Byte.BYTES;
            else if (type.isAssignableFrom(Short.class)) return Short.BYTES;
            else throw new IllegalStateException("Cannot determine byte size for data type: " + type);
        }
    }

    @FunctionalInterface
    public interface FieldMapper<T> extends BiConsumer<T, ByteBuffer> {
    }

    protected VertexArrayAccessor() {
        this.fields = new ArrayList<>();
        registerFields();
        this.elementByteSize = fields.stream()
                .mapToInt(FieldAccessor::getFieldByteSize)
                .sum();
    }

    protected <F> void addField(Class<F> type, FieldMapper<T> mapper) {
        addField(type, null, mapper);
    }

    protected <F> void addField(Class<F> type, @Nullable Class<? extends Number> componentType, FieldMapper<T> mapper) {
        addField(type, componentType, mapper, false);
    }

    protected <F> void addField(Class<F> type, FieldMapper<T> mapper, boolean normalised) {
        addField(type, null, mapper, normalised);
    }

    protected <F> void addField(Class<F> type, @Nullable Class<? extends Number> componentType, FieldMapper<T> mapper, boolean normalised) {
        fields.add(new FieldAccessor<>(type, componentType, mapper, normalised));
    }

    protected abstract void registerFields();

}
