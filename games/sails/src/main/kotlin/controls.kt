package io.github.etieskrill.games.sails

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.getComponent
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.window.Window

class PlayerShipControllerService(private val window: Window) : Service {

    override fun canProcess(entity: Entity) = entity.hasComponents(
        PlayerShipController::class.java,
        InputDirection::class.java
    )

    override fun process(
        targetEntity: Entity,
        entities: List<Entity?>,
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
        inputDirection.strength = 1f
    }

}