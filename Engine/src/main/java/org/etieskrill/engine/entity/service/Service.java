package org.etieskrill.engine.entity.service;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.system.EntitySystem;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;

/**
 * Processes all entities which possess a specific set of components.
 */
public interface Service {

    /**
     * Asks this service whether a set of components belonging to a single {@code entity} qualifies the entity for
     * processing by this service.
     *
     * @param entity components belonging to an entity
     * @return whether the entity can be processed
     */
    boolean canProcess(Entity entity);

    //TODO ordered processing
//    Comparator<Entity> comparator();

    /**
     * Called once before any entities are processed.
     *
     * @param entities all entities
     */
    default void preProcess(List<Entity> entities) {
    }

    /**
     * Called once after all entities are processed.
     *
     * @param entities all entities
     */
    default void postProcess(List<Entity> entities) {
    }

    /**
     * Tells the service to do its processing on the given entity. The entity is guaranteed to have the requisite
     * components, which are accessible via the component's type.
     *
     * @param targetEntity the entity
     * @param entities     all entities
     * @param delta        delta time of the last two frames
     */
    void process(Entity targetEntity, List<Entity> entities, double delta);

    /**
     * Specifies an absolute set of services which, if present, must be run after this service does its processing.
     *
     * @return set of (semi-) dependent services
     */
    default Set<Class<? extends Service>> runBefore() {
        return emptySet();
    }

    /**
     * Specifies an absolute set of services which, if present, must be run before this service does its processing.
     *
     * @return set of prerequisite services
     */
    default Set<Class<? extends Service>> runAfter() {
        return emptySet();
    }

    /**
     * Specifies a service's priority, which may be used to direct the {@link EntitySystem} to run this service at some
     * specific stage <i>without</i> having to set absolute dependencies in every other service.
     * <p>
     * This is not an absolute directive however, and will be overridden if a dependency in {@link #runAfter()} requires
     * it.
     *
     * @return the service's priority
     */
    default int priority() {
        return 0;
    }

}
