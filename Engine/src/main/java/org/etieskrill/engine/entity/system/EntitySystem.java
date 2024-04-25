package org.etieskrill.engine.entity.system;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.service.Service;

import java.util.ArrayList;
import java.util.List;

public class EntitySystem {

    private final ArrayList<Entity> entities;

    private final List<Service> services; //TODO replace with set when execution plan is established
    private final List<Service> serviceExecutionPlan;

    private int nextEntityId;

    public EntitySystem() {
        this.entities = new ArrayList<>();

        this.services = new ArrayList<>();
        this.serviceExecutionPlan = new ArrayList<>();

        this.nextEntityId = 0;
    }

    public Entity createEntity() {
        Entity entity = new Entity(nextEntityId++);
        if (entities.contains(entity))
            throw new IllegalStateException("Could not create new unique entity");

        entities.ensureCapacity(entity.getId());
        entities.add(entity.getId(), entity);
        return entity;
    }

    public boolean entityExists(Entity entity) {
        return entities.contains(entity);
    }

    public void addService(Service service) {
        services.add(service);
        createServiceExecutionPlan();
    }

    public void removeService(Service service) {
        services.remove(service);
        createServiceExecutionPlan();
    }

    private void createServiceExecutionPlan() {
        //TODO implement
        serviceExecutionPlan.clear();
        serviceExecutionPlan.addAll(services);
    }

    public void update(double delta) {
        //TODO processable caching
        serviceExecutionPlan.forEach(service -> {
            service.preProcess(entities);
            entities.forEach(entity -> {
                if (service.canProcess(entity)) {
                    service.process(entity, entities, delta);
                }
            });
        });
    }

}
