package org.etieskrill.game.horde.service

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.game.horde.component.EffectContainer
import org.etieskrill.game.horde.util.getComponent

class EffectService : Service {
    override fun canProcess(entity: Entity): Boolean = entity.hasComponents(EffectContainer::class.java)

    override fun process(targetEntity: Entity, entities: MutableList<Entity>, delta: Double) =
        targetEntity.getComponent<EffectContainer>()!!.update(delta)
}
