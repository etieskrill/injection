package io.github.etieskrill.games.sails

import org.etieskrill.engine.application.GameApplication
import org.etieskrill.engine.input.KeyInputManager
import org.joml.Vector2f
import org.joml.Vector4f

fun main() {
    Game.run()
}

data class NavalTransform(val position: Vector2f, val prevPosition: Vector2f, var rotation: Float) {
    constructor() : this(Vector2f(), Vector2f(), 0f)
}

data class InputDirection(val direction: Vector2f, var strength: Float) {
    constructor() : this(Vector2f(), 0f)
}

class PlayerShipController(var initialised: Boolean = false) : KeyInputManager()

object Game : GameApplication() {
    init {
        renderer.setClearColour(Vector4f(25f, 100f, 200f, 255f).div(255f))

        val playerShip = entitySystem.createEntity()
            .withComponent(NavalTransform())
            .withComponent(InputDirection())
            .withComponent(PlayerShipController())

        entitySystem.addService(PlayerShipControllerService(window))
        entitySystem.addService(ShipPhysicsService())
        entitySystem.addService(ShipRenderService(renderer, window))
    }

    override fun loop(delta: Double) {
    }
}
