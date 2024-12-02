package org.etieskrill.engine.entity.system;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Enabled;
import org.etieskrill.engine.entity.service.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class EntitySystem {

    private final ArrayList<Entity> entities;

    private final List<Service> services; //TODO replace with set when execution plan is established
    private final List<Service> serviceExecutionPlan;
    private final Map<Service, List<Entity>> orderedEntities;

    private int nextEntityId;

    private static final Logger logger = LoggerFactory.getLogger(EntitySystem.class);

    public EntitySystem() {
        this.entities = new ArrayList<>();

        this.services = new ArrayList<>();
        this.serviceExecutionPlan = new ArrayList<>();
        this.orderedEntities = new HashMap<>();

        this.nextEntityId = 0;
    }

    public Entity createEntity() {
        return createEntity(Entity::new);
    }

    public <E extends Entity> E createEntity(@NotNull Function<Integer, E> factory) {
        E entity = factory.apply(nextEntityId++);
        if (entities.contains(entity))
            throw new IllegalStateException("Could not create new unique entity");
        logger.debug("New entity with id '{}'", entity.getId());

        entities.ensureCapacity(entity.getId());
        entities.add(entity.getId(), entity);
        orderedEntities.forEach(((service, entities) -> entities.add(entity))); //TODO custom list impl with cached views?
        return entity;
    }

    public boolean entityExists(@Nullable Entity entity) {
        return entities.contains(entity);
    }

    public void addServices(Service @NotNull ... services) {
        for (Service service : services) {
            addService(service);
        }
    }

    public void addService(@NotNull Service service) {
        services.add(service);
        if (service.comparator() != null) {
            orderedEntities.put(service, new ArrayList<>());
        }
        createServiceExecutionPlan();
    }

    public void removeService(@NotNull Service service) {
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
            List<Entity> entities = orderedEntities.get(service);
            if (entities != null) {
                entities.sort(service.comparator());
            } else {
                entities = this.entities;
            }

            service.preProcess(entities); //TODO pass unmodifiable view and add removeEntity to system/service
            final List<Entity> finalEntities = entities;
            entities.forEach(entity -> {
                var enabled = entity.getComponent(Enabled.class);
                if (enabled != null && !enabled.isEnabled()) { //TODO add config toggle
                    return;
                }

                if (service.canProcess(entity)) {
                    service.process(entity, finalEntities, delta);
                }
            });
            service.postProcess(entities);
        });
    }

}
