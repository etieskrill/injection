package io.github.etieskrill.games.sails

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.service.Service
import org.joml.Matrix2f
import org.joml.Vector2f
import org.joml.minus
import org.joml.plus
import org.joml.times

class ShipPhysicsService : Service {

    override fun canProcess(entity: Entity) = entity.hasComponents(
        NavalTransform::class.java, InputDirection::class.java
    )

    override fun process(
        targetEntity: Entity,
        entities: List<Entity?>,
        delta: Double
    ) {
        val transform = targetEntity.getComponent(NavalTransform::class.java)!!
        val inputDirection = targetEntity.getComponent(InputDirection::class.java)!!

        transform.rotation += inputDirection.direction.x * delta.toFloat()
        println(transform.rotation)
        val orientedDirection = Matrix2f().rotation(-transform.rotation) * inputDirection.direction
        val newPosition = transform.position.mul(2f, Vector2f()) - transform.prevPosition +
                orientedDirection.mul(inputDirection.strength, Vector2f()).mul((delta * delta).toFloat(), Vector2f())

        transform.prevPosition.set(transform.position)
        transform.position.set(newPosition)
    }

}