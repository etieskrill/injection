package org.etieskrill.engine.entity.service.impl

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.component.WorldSpaceAABB
import org.etieskrill.engine.entity.service.Service
import org.joml.primitives.AABBf

class BoundingBoxService : Service {

    override fun canProcess(entity: Entity) = entity.hasComponents<Transform, AABBf, WorldSpaceAABB>()

    override fun process(targetEntity: Entity, entities: List<Entity>, delta: Double) {
        val transform = targetEntity.getComponent<Transform>()!!
        val aabb = targetEntity.getComponent<AABBf>()!!
        val worldSpaceAABB = targetEntity.getComponent<WorldSpaceAABB>()!!

        aabb.transform(transform.matrix, worldSpaceAABB)
    }

}
