package org.etieskrill.engine.entity.service.impl

import net.bifreus.kml.AABB
import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.component.WorldSpaceAABB
import org.etieskrill.engine.entity.service.Service

class BoundingBoxService : Service {

    override fun canProcess(entity: Entity) = entity.hasComponents<Transform, AABB, WorldSpaceAABB>()

    override fun process(targetEntity: Entity, entities: List<Entity>, delta: Double) {
        val transform = targetEntity.getComponent<Transform>()!!
        val aabb = targetEntity.getComponent<AABB>()!!
        val worldSpaceAABB = targetEntity.getComponent<WorldSpaceAABB>()!!

        worldSpaceAABB(transform.matrix * aabb)
    }

}
