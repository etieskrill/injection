package io.github.etieskrill.games.sails

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.getComponent
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.engine.input.KeyInputManager
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.window.Window
import org.joml.Matrix2f
import org.joml.Vector2f
import org.joml.dot
import org.joml.minus
import org.joml.times

data class InputDirection(val direction: Vector2f, var strength: Float) {
    constructor() : this(Vector2f(), 0f)
}

class PlayerShipController(var initialised: Boolean = false) : KeyInputManager()
class EnemyShipController //TODO behaviours

class PlayerShipControllerService(private val window: Window) : Service {

    override fun canProcess(entity: Entity) = entity.hasComponents(
        PlayerShipController::class.java,
        InputDirection::class.java
    )

    override fun process(
        targetEntity: Entity,
        entities: List<Entity>,
        delta: Double
    ) {
        val controller = targetEntity.getComponent<PlayerShipController>()!!
        val inputDirection = targetEntity.getComponent<InputDirection>()!!

        if (!controller.initialised) {
            window.addKeyInputs(controller)
            controller.initialised = true
        }

        inputDirection.direction.zero()

        if (controller.isPressed(Keys.W)) inputDirection.direction.y += 1
        if (controller.isPressed(Keys.S)) inputDirection.direction.y -= 1

        if (controller.isPressed(Keys.A)) inputDirection.direction.x -= 1
        if (controller.isPressed(Keys.D)) inputDirection.direction.x += 1

//        inputDirection.direction.normalize() //FIXME i hate Hate HATE the way this produces NaN on all zeroes
        if (!inputDirection.direction.equals(0f, 0f)) inputDirection.direction.normalize()
        inputDirection.strength = 10f
    }

}

class EnemyShipControllerService : Service {

    override fun canProcess(entity: Entity) = entity.hasComponents(
        NavalTransform::class.java,
        EnemyShipController::class.java,
        InputDirection::class.java
    )

    override fun process(targetEntity: Entity, entities: MutableList<Entity>, delta: Double) {
        val transform = targetEntity.getComponent<NavalTransform>()!!
        val input = targetEntity.getComponent<InputDirection>()!!

        val playerShipDirections = entities
            .filter { it.hasComponents(PlayerShipController::class.java) }
            .associateWith { it.getComponent<NavalTransform>()!!.position - transform.position }

        val (_, targetDirection) = playerShipDirections.minBy { it.value.length() }

        val rotation = targetDirection.normalize(Vector2f()) dot
                (Matrix2f().rotation(transform.rotation) * Vector2f(-1f, 0f))

        input.direction.set(rotation, targetDirection.length() / 1000 + 0.5f)
        input.strength = if (targetDirection.length() < 1000) 10f else 0f
    }

}
