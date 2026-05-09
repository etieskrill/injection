package org.etieskrill.engine.entity.service.impl

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.Scripts
import org.etieskrill.engine.entity.service.Service

class SnippetsService : Service {

    override fun canProcess(entity: Entity) = entity.hasComponents<Scripts>()

    override fun process(targetEntity: Entity, entities: List<Entity>, delta: Double) {
        targetEntity.getComponent<Scripts>()!!.update(delta)
    }

}
