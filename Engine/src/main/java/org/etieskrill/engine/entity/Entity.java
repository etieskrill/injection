package org.etieskrill.engine.entity;

import kotlin.Deprecated;
import lombok.Getter;
import lombok.ToString;
import org.etieskrill.engine.Disposable;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.etieskrill.engine.util.ClassUtils.getSimpleName;

@ToString
public class Entity implements Disposable {

    private final @Getter int id;
    private final Map<Class<?>, Object> components;
    private boolean valid;

    public Entity(int id) {
        this.id = id;
        this.components = new HashMap<>();
        this.valid = false;
    }

    public <T> @Nullable T getComponent(Class<T> type) {
        return (T) components.get(type);
    }

    public <T> T addComponent(T component) {
        checkValid();

        if (components.putIfAbsent(component.getClass(), component) != null)
            throw new IllegalStateException("Entity already has component of type '" + getSimpleName(component) + "'");

        return component;
    }

    protected <T> T addComponentNoCheck(T component) {
        if (components.putIfAbsent(component.getClass(), component) != null)
            throw new IllegalStateException("Entity already has component of type '" + getSimpleName(component) + "'");

        return component;
    }

    public Entity withComponent(Object component) {
        checkValid();

        if (components.putIfAbsent(component.getClass(), component) != null)
            throw new IllegalStateException("Entity already has component of type '" + getSimpleName(component) + "'");

        return this;
    }

    public boolean hasComponents(Class<?>... components) {
        checkValid();
        return this.components.keySet().containsAll(Set.of(components));
    }

    public boolean isValid() {
        return valid;
    }

    @Deprecated(message = "Public because Java access qualifiers suck. Do not use.")
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    private void checkValid() {
        if (!isValid())
            throw new IllegalStateException("Entity with id " + id + " is not valid and cannot be modified");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return id == entity.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public void dispose() {
        for (Object component : components.values()) {
            if (component instanceof Disposable) {
                ((Disposable) component).dispose();
            }
        }
    }

}
