package org.etieskrill.game.horde.component

import org.etieskrill.engine.entity.Entity
import org.etieskrill.game.horde.util.getComponent

class EffectContainer(private val effects: MutableList<Effect> = mutableListOf()) {
    fun add(effect: Effect, entity: Entity) {
        effects.find { it.sourceId == effect.sourceId }?.let { it.onRemove(); effects.remove(it) }

        effect.onAdd(entity)
        effects.add(effect)
    }

    fun update(delta: Double) =
        effects.forEach {
            if (it.update(delta)) {
                it.onRemove()
                effects.remove(it)
            }
        }
}

interface Effect {
    val sourceId: String?

    fun onAdd(entity: Entity)
    fun update(delta: Double): Boolean
    fun onRemove()
}

data class SlowEffect(private val magnitude: Float, private var stacks: Int, override val sourceId: String?) : Effect {
    private var movementSpeed: MovementSpeed? = null

    override fun onAdd(entity: Entity) {
        movementSpeed = entity.getComponent<MovementSpeed>()
        movementSpeed?.apply { factor /= magnitude }
    }

    override fun update(delta: Double): Boolean {
        stacks--
        return stacks <= 0
    }

    override fun onRemove() {
        movementSpeed?.apply { factor *= magnitude }
    }
}
