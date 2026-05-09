package org.etieskrill.engine.entity.service

import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.entity.Entity
import kotlin.reflect.KClass

/**
 * Processes all entities which possess a specific set of components.
 */
interface Service : Disposable {

    /**
     * Asks this service whether a set of components belonging to a single [entity] qualifies the entity for
     * processing by this service.
     *
     * @param entity components belonging to an entity
     * @return whether the entity can be processed
     */
    fun canProcess(entity: Entity): Boolean

    /**
     * Optionally specifies the order in which entities are [processed](process).
     *
     * @return an optional process ordering
     */
    val comparator: Comparator<Entity>? get() = null

    /**
     * Tells the service to do its processing on the given entity. The entity is guaranteed to have the requisite
     * components, which are accessible via the component's type. Entities are presented in the order specified by the
     * [comparator], if any is set.
     *
     * @param targetEntity the entity
     * @param entities     all entities
     * @param delta        delta time of the last two frames
     */
    fun process(targetEntity: Entity, entities: List<Entity>, delta: Double)

    /**
     * Called once before any entities are processed.
     *
     * @param entities all entities
     */
    fun preProcess(delta: Double, entities: List<Entity>) = Unit

    /**
     * Called once after all entities are processed.
     *
     * @param entities all entities
     */
    fun postProcess(entities: List<Entity>) = Unit

    /**
     * Called if an entity is removed from the system at the end of entity processing.
     *
     * @param entity the entity that was removed
     */
    fun entityRemoved(entity: Entity) = Unit

    /**
     * Specifies an absolute set of services which, if present, must be run after this service does its processing.
     *
     * @return set of (semi-) dependent services
     */
    val runBefore: Set<KClass<out Service>> get() = setOf()

    /**
     * Specifies an absolute set of services which, if present, must be run before this service does its processing.
     *
     * @return set of prerequisite services
     */
    val runAfter: Set<KClass<out Service>> get() = setOf()

    /**
     * Specifies a service's priority, which may be used to direct the
     * [EntitySystem](org.etieskrill.engine.entity.system.EntitySystem) to run this service at some specific stage
     * <i>without</i> having to set absolute dependencies in every other service.
     *
     * This is not an absolute directive however, and will be overridden if a dependency in [runAfter] requires it.
     *
     * @return the service's priority
     */
    val priority: Int get() = 0

    override fun dispose() = Unit

}
