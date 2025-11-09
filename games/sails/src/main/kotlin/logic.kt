package io.github.etieskrill.games.sails

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.getComponent
import org.etieskrill.engine.entity.service.Service
import kotlin.math.min

class ShipCollisionService : Service {

    override fun canProcess(entity: Entity) = entity.hasComponents(
        NavalTransform::class.java,
        ShipCollider::class.java,
        ShipStats::class.java
    )

    override fun process(targetEntity: Entity, entities: MutableList<Entity>, delta: Double) {
        val transform = targetEntity.getComponent<NavalTransform>()!!
        val collider = targetEntity.getComponent<ShipCollider>()!!
        val stats = targetEntity.getComponent<ShipStats>()!!

        collider.collisions.forEach {
            val damage = it.speed * it.entity.getComponent<ShipStats>()!!.rammingDamageModifier *
                    min(1f, it.entity.getComponent<NavalTransform>()!!.mass / transform.mass)
            stats.currentHealth -= (10 * damage * damage).toInt()
        }
    }

}
