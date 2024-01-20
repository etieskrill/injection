package org.etieskrill.engine.entity;

import org.etieskrill.engine.entity.data.Component;
import org.etieskrill.engine.entity.system.EntitySystem;

import java.util.*;

public class Controller {

    private int currentId;
    private final Map<Entity, List<Component>> entities;
    private final List<Component> components;
    private final List<EntitySystem> systems;

    public Controller() {
        this.currentId = 0;
        this.entities = new HashMap<>();
        this.components = new ArrayList<>();
        this.systems = new ArrayList<>();
    }

    public void update(float delta) {
        for (EntitySystem system : systems) {
            system.update(delta, entities);
        }
    }

    public Entity createEntity() {
        Entity entity = new Entity(currentId++);
        entities.put(entity, new ArrayList<>());
        return entity;
    }

    public void addComponents(Entity entity, Component... components) {
    }

    public List<Component> getComponents(Entity entity) {
        return entities.get(entity);
    }

    public List<Component> getComponentsOfType(Class<?> type) {
        return components.stream().filter(component -> component.getClass().equals(type)).toList();
    }

}
