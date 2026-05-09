package org.etieskrill.engine.entity.system;

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.Enabled
import org.etieskrill.engine.entity.service.Service

private val logger = KotlinLogging.logger {}

class EntitySystem : Disposable {

    private val entities = mutableListOf<Entity>()

    private val services = mutableListOf<Service>() //TODO replace with set when execution plan is established
    private val serviceExecutionPlan = mutableListOf<Service>()
    private val orderedEntities = mutableMapOf<Service, MutableList<Entity>>()

    private var nextEntityIndex = 0
    private val freeIndices = mutableListOf<Int>()
    private val addedEntities = mutableListOf<Entity>()
    private val markedForRemoval = mutableListOf<Entity>()

    private var disposed = false

    fun <T : Entity> constructEntity(block: (Int) -> T): T {
        val entity = block(getNextId())
        logger.debug { "New entity with id '${entity.id}'" }

        addedEntities += entity

        return entity
    }

    fun createEntity(block: Entity.() -> Unit): Entity {
        val entity = Entity(getNextId()).apply(block)
        logger.debug { "New entity with id '${entity.id}'" }

        addedEntities += entity

        return entity
    }

    private fun getNextId(): Int {
        val nextId = when {
            freeIndices.isNotEmpty() -> freeIndices.removeFirst()
            entities.size + addedEntities.size == nextEntityIndex -> nextEntityIndex++
            else -> error("Failed to create id for new entity")
        }

        check(entities.none { it.id == nextId }) { "Tried to create entity with id of an existing entity" }

        return nextId
    }

    fun entityExists(entity: Entity?): Boolean = entities.contains(entity)

    fun removeEntity(entity: Entity) = markedForRemoval.add(entity)

    fun removeEntity(entityId: Int) = markedForRemoval.add(
        entities.find { it.id == entityId } ?: error("Entity with id $entityId does not exist")
    )

    fun isMarkedForRemoval(entity: Entity) = entity in markedForRemoval

    fun addService(service: Service) {
        services += service
        service.comparator?.let {
            orderedEntities[service] = mutableListOf()
        }
        createServiceExecutionPlan()
    }

    fun addServices(vararg services: Service) = services.forEach { addService(it) }

    fun removeService(service: Service) {
        services -= service
        createServiceExecutionPlan()
    }

    private fun createServiceExecutionPlan() {
        //TODO implement
        serviceExecutionPlan.clear()
        serviceExecutionPlan.addAll(services)
    }

    fun update(delta: Double) {
        //TODO processable caching
        serviceExecutionPlan.forEach { service ->
            val entities =
                (service.comparator?.let { orderedEntities[service]!!.sortedWith(it) }
                    ?: this.entities.toList())

            service.preProcess(delta, entities)
            entities.filterNot { it.getComponent<Enabled>()?.enabled == false }
                .filter { service.canProcess(it) }
                .forEach { service.process(it, entities, delta) }
            service.postProcess(entities)
        }

        entities.addAll(addedEntities)
        entities.sortBy(Entity::id)
        orderedEntities.forEach { _, entities -> entities.addAll(addedEntities) }
        addedEntities.clear()

        entities.removeAll(markedForRemoval)
        entities.sortBy(Entity::id)
        orderedEntities.forEach { _, entities -> entities.removeAll(markedForRemoval) }
        freeIndices.addAll(markedForRemoval.map(Entity::id))
        markedForRemoval.forEach(Entity::dispose)
        markedForRemoval.clear()
    }

    override fun dispose() {
        if (disposed) return
        entities.forEach(Entity::dispose)
        services.forEach(Service::dispose)
        disposed = true
    }

}
