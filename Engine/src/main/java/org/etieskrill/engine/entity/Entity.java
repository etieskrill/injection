package org.etieskrill.engine.entity;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.etieskrill.engine.util.ClassUtils.getSimpleName;

@ToString
public class Entity {

    private final @Getter int id;
    private final Map<Class<?>, Object> components;

    public Entity(int id) {
        this.id = id;
        this.components = new HashMap<>();
    }

    public <T> @Nullable T getComponent(Class<T> type) {
        return (T) components.get(type);
    }

    public <T> T addComponent(T component) {
        if (components.putIfAbsent(component.getClass(), component) != null)
            throw new IllegalStateException("Entity already has component of type '" + getSimpleName(component) + "'");

        return component;
    }

    public Entity withComponent(Object component) {
        if (components.putIfAbsent(component.getClass(), component) != null)
            throw new IllegalStateException("Entity already has component of type '" + getSimpleName(component) + "'");

        return this;
    }

    public boolean hasComponents(Class<?>... components) {
        return this.components.keySet().containsAll(Set.of(components));
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

}
