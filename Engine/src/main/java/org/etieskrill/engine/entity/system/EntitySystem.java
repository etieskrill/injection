package org.etieskrill.engine.entity.system;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.service.GroupableService;
import org.etieskrill.engine.entity.service.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class EntitySystem {

    private final ArrayList<Entity> entities;

    private final List<Service> services; //TODO replace with set when execution plan is established
    private final List<Service> serviceExecutionPlan;

    private int nextEntityId;

    private static final Logger logger = LoggerFactory.getLogger(EntitySystem.class);

    public EntitySystem() {
        this.entities = new ArrayList<>();

        this.services = new ArrayList<>();
        this.serviceExecutionPlan = new ArrayList<>();

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
        return entity;
    }

    public boolean entityExists(@Nullable Entity entity) {
        return entities.contains(entity);
    }

    public void addService(@NotNull Service service) {
        services.add(service);
        createServiceExecutionPlan();
    }

    public void removeService(@NotNull Service service) {
        services.remove(service);
        createServiceExecutionPlan();
    }

    private void createServiceExecutionPlan() {
        //TODO implement
        // - check for cyclic inner services etc.
        serviceExecutionPlan.clear();
        serviceExecutionPlan.addAll(services);
    }

    public void update(double delta) {
        //TODO processable caching
        serviceExecutionPlan.forEach(service -> {
            updateService(delta, service);
        });
    }

    private void updateService(double delta, Service service) {
        service.preProcess(entities);
        if (service instanceof GroupableService groupableService) {
            groupableService.getPreServices().forEach(innerService -> updateService(delta, innerService));
        }

        entities.stream()
                .filter(service::canProcess)
                .forEach(entity -> {
                    service.process(entity, entities, delta);
                });

        if (service instanceof GroupableService groupableService) {
            groupableService.getPostServices().forEach(innerService -> updateService(delta, innerService));
        }
        service.postProcess(entities);
    }

}
