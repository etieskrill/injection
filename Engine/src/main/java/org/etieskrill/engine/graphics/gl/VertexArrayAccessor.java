package org.etieskrill.engine.graphics.gl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static lombok.AccessLevel.PACKAGE;

/**
 * @param <T> type of data accessed
 */
public abstract class VertexArrayAccessor<T> {

    protected final @Getter(PACKAGE) List<FieldAccessor<?>> fields;

    @Getter
    @AllArgsConstructor
    protected final class FieldAccessor<F> {
        private final Class<F> type;
        private final @Nullable Class<? extends Number> componentType;
        private final Function<T, F> accessor;
        private final boolean normalised;
    }

    protected VertexArrayAccessor() {
        this.fields = new ArrayList<>();
        registerFields();
    }

    protected <F> void addField(Class<F> type, Function<T, F> fieldAccessor) {
        addField(type, null, fieldAccessor);
    }

    protected <F> void addField(Class<F> type, @Nullable Class<? extends Number> componentType, Function<T, F> fieldAccessor) {
        addField(type, componentType, fieldAccessor, false);
    }

    protected <F> void addField(Class<F> type, Function<T, F> fieldAccessor, boolean normalised) {
        addField(type, null, fieldAccessor, normalised);
    }

    protected <F> void addField(Class<F> type, @Nullable Class<? extends Number> componentType, Function<T, F> fieldAccessor, boolean normalised) {
        fields.add(new FieldAccessor<>(type, componentType, fieldAccessor, normalised));
    }

    protected abstract void registerFields();

}
