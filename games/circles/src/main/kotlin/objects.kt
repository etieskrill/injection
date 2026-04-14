package io.github.etieskrill.games.circles

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.getComponent
import org.etieskrill.engine.entity.service.Service
import org.joml.Math.clamp

data class Heatable(
    val mass: Float,
    val specificHeat: Float = 1f,
    /**
     * Use [energy] for normal interactions instead. Setting this sets the entity's absolute temperature.
     */
    var temperature: Float = 0f,
    /**
     * Accumulates the amount of energy transferred from/to the entity in a frame. Affects [temperature] when this
     * component is processed.
     */
    var energy: Float = 0f
)

class HeatService(val environment: Environment) : Service {
    override fun canProcess(entity: Entity) = entity.hasComponents(Heatable::class.java)

    override fun process(targetEntity: Entity, entities: List<Entity>, delta: Double) {
        val heatable = targetEntity.getComponent<Heatable>()!!

        //Q: energy, m: mass, c: specific heat, T: temperature
        //Q = m c dT
        //dT = Q / m c
        //waow

        heatable.temperature += (heatable.energy / (heatable.mass * heatable.specificHeat)) * delta.toFloat()
        heatable.energy = 0f

        val transform = targetEntity.getComponent<Transform>() ?: return

        val envTemperature = environment.getTemperature(transform.position)

        val heatTransferCoefficient = 1f //could include object geometry (so trial and error), water contact, wind...
        val coolingConstant = clamp(
            0f, 1f, heatTransferCoefficient / (heatTransferCoefficient + (heatable.mass * heatable.specificHeat))
        )
        val convectionTransfer = -coolingConstant * (heatable.temperature - envTemperature) * delta.toFloat()

        heatable.temperature += convectionTransfer
        environment.addHeatEnergy(transform.position, heatable.mass * heatable.specificHeat * -convectionTransfer)
    }
}
