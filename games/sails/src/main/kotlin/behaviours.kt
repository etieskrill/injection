package io.github.etieskrill.games.sails

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.getComponent
import org.joml.Math.*
import org.joml.Matrix2f
import org.joml.Vector2f
import org.joml.dot
import org.joml.minus
import org.joml.times
import kotlin.math.atan2
import kotlin.math.sign

class PrimitiveBehaviour : Behaviour {
    override fun getMoveVector(
        entity: Entity,
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
    val broadsideArc: Float = toRadians(30f),
    val chargeDistance: Float = 1.5f * maxDistance,
    val approachAngle: Float = toRadians(15f),
    val circleAngle: Float = toRadians(approachAngle / 0.6f)
    //TODO min/max approach angle, generic input scale control; if possible invariant with size
    //TODO target threat eval, retreat, general threat eval, group cohesion...
) : Behaviour {
    // stages:
    // - [close distance] if distance > chargeDistance, direct charge
    // - [approach] if distance > max, angled approach
    // - [circle] if min < distance < max, train broadside (generally primary weapon arcs), vary speed depending on distance and angle to target
    // - [retreat] if distance < min, angled retreat
    // vary strength with distance to desired angle to target in order to maximise turn rate
    enum class Stage { CLOSE_DISTANCE, APPROACH, CIRCLE, RETREAT }

    override fun getMoveVector(
        entity: Entity,
        transform: NavalTransform,
        otherEntities: Map<Entity, NavalTransform>
    ): Pair<Vector2f, Float> {
        val (distance, direction) = otherEntities
            .filter { it.key.hasComponents(PlayerShipController::class.java) }
            .mapValues { it.key.getComponent<NavalTransform>()!!.position - transform.position }
            .mapKeys { it.value.length() }
            .minByOrNull { it.key }
            ?: return Pair(Vector2f(0f), 0f)

        val stage = when {
            distance > chargeDistance -> Stage.CLOSE_DISTANCE
            distance > maxDistance -> Stage.APPROACH
            distance in minDistance..maxDistance -> Stage.CIRCLE
            distance < minDistance -> Stage.RETREAT
            else -> error("dis shit not cash money dawg")
        }

        val deviation = (transform.rotation - ((atan2(direction.y, direction.x) - PI_OVER_2_f))).radianToPolar()
        val inBroadsideArc = abs(deviation) in (PI_OVER_2_f - broadsideArc / 2f)..(PI_OVER_2_f + broadsideArc / 2f)
        //how much the ship wants to rotate away counter-clockwise from the target
        val desiredRotation: Float = when (stage) {
            Stage.CLOSE_DISTANCE -> 0f
            Stage.APPROACH -> -approachAngle * deviation.sign
            Stage.CIRCLE -> (if (inBroadsideArc) -circleAngle else -PI_OVER_2_f) * deviation.sign
            Stage.RETREAT -> PI_f
        }

        val desiredMoveDirection = Vector2f(
            -(direction.normalize(Vector2f())
                    dot Matrix2f().rotation(transform.rotation + desiredRotation) * Vector2f(1f, 0f)), 1f
        )

        val angleDeviation = ((transform.rotation - (atan2(direction.y, direction.x) + desiredRotation - PI_OVER_2_f)))
            .radianToPolar()
        val turnFactor = 1 - abs(angleDeviation / PI_f)

//        val strength = clamp(0f, 10f, 10f * ((2000f - distance) / 2000f + 0.5f))
        val strength = lerp(2f, 12f, turnFactor).coerceIn(0f, 10f) * //would the name clamp have been to easy?
                if (stage == Stage.CIRCLE && inBroadsideArc) 0.3f else 1f

        return desiredMoveDirection to strength
    }
}

fun Float.radianToPolar() = mod(PI_TIMES_2_f).let {
    if (it <= PI_f) it
    else it - PI_TIMES_2_f
}
