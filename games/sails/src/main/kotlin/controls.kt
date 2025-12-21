package io.github.etieskrill.games.sails

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.getComponent
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.engine.input.KeyInputManager
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.window.Window
import org.joml.Math.*
import org.joml.Matrix2f
import org.joml.Vector2f
import org.joml.dot
import org.joml.minus
import org.joml.times

data class InputDirection(val direction: Vector2f, var strength: Float) {
    constructor() : this(Vector2f(), 0f)
}

class PlayerShipController(var initialised: Boolean = false) : KeyInputManager()

class EnemyShipController(val behaviour: Behaviour) {
    interface Behaviour {
        fun getMoveVector(transform: NavalTransform, otherEntities: Map<Entity, NavalTransform>): Pair<Vector2f, Float>
    }

    object Behaviours {
        class Primitive : Behaviour {
            override fun getMoveVector(
                transform: NavalTransform,
                otherEntities: Map<Entity, NavalTransform>
            ): Pair<Vector2f, Float> {
                val playerShipDirections = otherEntities
                    .filter { it.key.hasComponents(PlayerShipController::class.java) }
                    .mapValues { it.key.getComponent<NavalTransform>()!!.position - transform.position }

                val (_, targetDirection) = playerShipDirections.minByOrNull { it.value.length() }
                    ?: return Pair(Vector2f(0f), 0f)

                val rotation = targetDirection.normalize(Vector2f()) dot
                        (Matrix2f().rotation(transform.rotation) * Vector2f(-1f, 0f))

                val direction = Vector2f(rotation, targetDirection.length() / 1000 + 0.5f)
                val strength = if (targetDirection.length() < 1000f) 10f else 0f

                return direction to strength
            }
        }

        class CircleBehaviour(
            val minDistance: Float,
            val maxDistance: Float = minDistance,
            //TODO min/max approach angle, generic input scale control; if possible invariant with size
            //TODO target threat eval, retreat, general threat eval, group cohesion...
        ) : Behaviour {
            override fun getMoveVector(
                transform: NavalTransform,
                otherEntities: Map<Entity, NavalTransform>
            ): Pair<Vector2f, Float> {
                val playerShipDirections = otherEntities
                    .filter { it.key.hasComponents(PlayerShipController::class.java) }
                    .mapValues { it.key.getComponent<NavalTransform>()!!.position - transform.position }

                val (distance, direction) = playerShipDirections
                    .mapKeys { it.value.length() }
                    .minByOrNull { it.key }
                    ?: return Pair(Vector2f(0f), 0f)

                val rotation = when {
                    distance < minDistance -> direction.normalize(Vector2f()) dot
                            (Matrix2f().rotation(transform.rotation + PI_f) * Vector2f(-1f, 0f))

                    distance in minDistance..maxDistance -> {
                        //FIXME correct wrapping/close side prio
                        val dot = direction.normalize(Vector2f()) dot
                                (Matrix2f().rotation(transform.rotation + PI_OVER_2_f) * Vector2f(-1f, 0f))
                        if (dot >= 0) dot else -dot
                    }

                    distance > maxDistance -> direction.normalize(Vector2f()) dot
                            (Matrix2f().rotation(transform.rotation) * Vector2f(-1f, 0f))

                    else -> error("dis shit not cash money dawg")
                }

                //TODO quick turn (lower strength) if distance too great or small
                val dir = Vector2f(rotation, 1f)
                val strength = clamp(0f, 10f, 10f * ((2000f - distance) / 2000f + 0.5f))

                return dir to strength
            }
        }
    }
}

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
        val controller = targetEntity.getComponent<EnemyShipController>()!!

        val otherEntities = entities
            .filter { it != targetEntity }
            .withComponents<NavalTransform>()

        val (direction, strength) = controller.behaviour.getMoveVector(transform, otherEntities)

        input.direction.set(direction)
        input.strength = strength
    }

}
