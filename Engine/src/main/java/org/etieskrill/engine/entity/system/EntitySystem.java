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

import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparing;

public class EntitySystem {

    private final ArrayList<Entity> entities;

    private final List<Service> services; //TODO replace with set when execution plan is established
    private final List<Service> serviceExecutionPlan;
    private final Map<Service, List<Entity>> orderedEntities;

    private int nextEntityIndex;
    private final List<Integer> freeIndices;
    private final List<Entity> addedEntities;
    private final List<Entity> markedForRemoval;

    private static final Logger logger = LoggerFactory.getLogger(EntitySystem.class);

    public EntitySystem() {
        this.entities = new ArrayList<>();

        this.services = new ArrayList<>();
        this.serviceExecutionPlan = new ArrayList<>();
        this.orderedEntities = new HashMap<>();

        this.nextEntityIndex = 0;
        this.freeIndices = new ArrayList<>();
        this.addedEntities = new ArrayList<>();
        this.markedForRemoval = new ArrayList<>();
    }

    public Entity createEntity() {
        return createEntity(Entity::new);
    }

    public <E extends Entity> E createEntity(@NotNull Function<Integer, E> factory) {
        int nextId;
        if (!freeIndices.isEmpty()) {
            nextId = freeIndices.removeFirst();
        } else if (entities.size() + addedEntities.size() == nextEntityIndex) {
            nextId = nextEntityIndex++;
        } else {
            throw new IllegalStateException("Failed to create id for new entity");
        }

        for (Entity entity : entities) {
            if (entity.getId() == nextId)
                throw new IllegalStateException("Tried to create entity with id of an existing entity");
        }

        E entity = factory.apply(nextId); //FIXME would not allow modification in factory function, but this should be removed anyway
        if (entities.contains(entity))
            throw new IllegalStateException("Could not create new unique entity");
        logger.debug("New entity with id '{}'", entity.getId());

        addedEntities.add(entity);
        entity.setValid(true);
        return entity;
    }

    /**
     * Get an unmodifiable view of all entities. Should only be used for prototyping, where implementing a
     * {@link Service} is not needed.
     *
     * @return an unmodifiable view of all entities
     */
    public @NotNull List<@NotNull Entity> getEntities() {
        return unmodifiableList(entities);
    }

    public boolean entityExists(@Nullable Entity entity) {
        return entities.contains(entity);
    }

    public void removeEntity(@NotNull Entity entity) {
        markedForRemoval.add(entity);
    }

    public void removeEntity(int entityId) {
        for (Entity entity : entities) {
            if (entity.getId() == entityId) {
                removeEntity(entity);
                return;
            }
        }
        throw new IllegalArgumentException("Entity with id " + entityId + " does not exist");
    }

    public boolean isMarkedForRemoval(@NotNull Entity entity) {
        return markedForRemoval.contains(entity);
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
            entities = unmodifiableList(entities);

            service.preProcess(entities);
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

        for (Entity entity : addedEntities) {
            entities.add(entity);
            entities.sort(comparing(Entity::getId));
            orderedEntities.forEach(((_, entities) -> entities.add(entity))); //TODO custom list impl with cached views?
        }
        addedEntities.clear();

        for (Entity removedEntity : markedForRemoval) {
            removedEntity.setValid(false);
            entities.remove(removedEntity);
            orderedEntities.forEach(((service, entities) -> {
                entities.remove(removedEntity);
                if (service.comparator() != null) entities.sort(service.comparator());
                else entities.sort(comparing(Entity::getId));
            }));
            freeIndices.add(removedEntity.getId());
        }
        entities.sort(comparing(Entity::getId));
        markedForRemoval.clear();
    }

}
