package org.etieskrill.engine.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.etieskrill.engine.util.ClassUtils.getSimpleName;

public class Entity {

    private final int id;
    private final Map<Class<?>, Object> components;

    public Entity(int id) {
        this.id = id;
        this.components = new HashMap<>();
    }

    public int getId() {
        return id;
    }

    public <T> T getComponent(Class<T> type) {
        return (T) components.get(type);
    }

    public void addComponent(Object component) {
        if (components.putIfAbsent(component.getClass(), component) != null)
            throw new IllegalStateException("Entity already has component of type '" + getSimpleName(component) + "'");
    }

    public boolean hasComponents(Class<?>... components) {
        return this.components.keySet().containsAll(Set.of(components));
    }

    @Override
    public String toString() {
        return "Entity{" +
                "id=" + id +
                ", components=" + components.keySet() +
                '}';
    }

}
