package io.github.etieskrill.games.sails

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.getComponent
import org.etieskrill.engine.entity.service.Service
import org.joml.Math
import org.joml.Matrix2f
import org.joml.Vector2f
import org.joml.minus
import org.joml.minusAssign
import org.joml.plus
import org.joml.plusAssign
import org.joml.times
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign

data class NavalTransform(
    val position: Vector2f = Vector2f(),
    var rotation: Float = 0f,

    val size: Float = 100f,
    val mass: Float = 100f,

    val prevPosition: Vector2f = Vector2f(position),
    var prevRotation: Float = rotation,

    val frontalDrag: Float = 0.25f,
    val lateralDrag: Float = 1.5f,
)

data class ShipCollision(val otherEntity: Entity, val speed: Float) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is ShipCollision) return false
        return otherEntity == other.otherEntity
    }

    override fun hashCode() = otherEntity.hashCode()

    override fun toString() = "ShipCollision(entity=${otherEntity.id}, speed=$speed)"
}

data class ShipCollider(
    //TODO custom collider shape
    val collisions: MutableSet<ShipCollision> = mutableSetOf()
)

class ShipPhysicsService : Service {

    override fun canProcess(entity: Entity) = entity.hasComponents(
        NavalTransform::class.java, InputDirection::class.java
    )

    override fun preProcess(entities: MutableList<Entity>) {
        entities.getComponents<ShipCollider>()
            .forEach { it.collisions.clear() }
    }

    override fun process(
        targetEntity: Entity,
        entities: List<Entity>,
        delta: Double
    ) {
        val stats = targetEntity.getComponent<ShipStats>()!!
        if (stats.state == ShipStats.State.DEAD) return

        val transform = targetEntity.getComponent(NavalTransform::class.java)!!
        val inputDirection = targetEntity.getComponent(InputDirection::class.java)!!

        inputDirection.direction.max(Vector2f(-1f))
        inputDirection.direction.min(Vector2f(1f))

        updatePosition(transform, inputDirection, delta)
        if (stats.state == ShipStats.State.ALIVE) {
            targetEntity.getComponent<ShipCollider>()
                ?.also { doCollisions(targetEntity, transform, it, entities, delta) }
        }
    }

    private fun updatePosition(transform: NavalTransform, inputDirection: InputDirection, delta: Double) {
        val relativeSpeed = Matrix2f().rotation(-transform.rotation) * (transform.position - transform.prevPosition)

        val angularSpeed = transform.rotation - transform.prevRotation
        val angularDrag =
            max(0f, (transform.lateralDrag * 997 * Math.toDegrees(angularSpeed).pow(2) * transform.size) / 100000)

        val angularStationaryDrag =
            min(1f, max(0f, abs(relativeSpeed.y)).pow(2)) //imitate that rudder does nothing when stationary
        val angularMovementDrag = min(
            1f, max(
                0f,
                transform.lateralDrag * relativeSpeed.y * relativeSpeed.y * (transform.size / 100f).pow(2)
            ) / 2 / 2
        )

        val newRotation = 2 * transform.rotation - transform.prevRotation +
                (inputDirection.direction.x * angularStationaryDrag
                        + (angularDrag + angularMovementDrag) * -sign(angularSpeed)) * (delta * delta).toFloat()

        val rotate = Matrix2f().rotation(newRotation)

        //dragForce = dragCoefficient * fluidDensity * flowSpeed^2 * area #(reference area == projected frontal area) / 2
        val lateralDragForce = transform.lateralDrag *
                997 * //density water
                relativeSpeed.x * relativeSpeed.x *
                (transform.size / 100f).pow(2) / 2 //very rough approximation for sideways cross-sectional area

        val frontalDragForce = transform.frontalDrag *
                997 *
                relativeSpeed.y * relativeSpeed.y *
                (transform.size / 500f).pow(2) / 2 //very rough approximation for frontal cross-sectional area

        val newPosition = transform.position.mul(2f, Vector2f()) - transform.prevPosition +
                (
                        (rotate * Vector2f(0f, inputDirection.direction.y * inputDirection.strength)) +
                                rotate.times(Vector2f(if (relativeSpeed.x > 0) -1f else 1f, 0f)).mul(lateralDragForce) +
                                rotate.times(Vector2f(0f, if (relativeSpeed.y > 0) -1f else 1f)).mul(frontalDragForce)
                        ).mul((delta * delta).toFloat(), Vector2f())

        transform.prevRotation = transform.rotation
        transform.rotation = newRotation
        transform.prevPosition.set(transform.position)
        transform.position.set(newPosition)
    }

    private fun doCollisions(
        entity: Entity,
        transform: NavalTransform,
        collider: ShipCollider,
        entities: List<Entity>,
        delta: Double
    ) {
        entities.filter { it != entity }.zip(
            entities.filter { it != entity }
                .getComponents3<NavalTransform, ShipCollider, ShipStats>()
        ).forEach { (otherEntity, components) ->
            val (otherTransform, otherCollider, otherStats) = components

            if (otherStats.state != ShipStats.State.ALIVE) return@forEach

            val distance = transform.position - otherTransform.position
            if (distance.length() > (transform.size + otherTransform.size) * 0.5f) return@forEach

            val overlap = (transform.size + otherTransform.size) * 0.5f - distance.length()
            val force =
                distance.max(Vector2f(0.1f), Vector2f()).normalize().mul((delta * delta).toFloat() * 1000 * overlap)
            transform.position.plusAssign(force * (otherTransform.mass / transform.mass))
            otherTransform.position.minusAssign(force * (transform.mass / otherTransform.mass))

            if (entity in otherCollider.collisions.map { it.otherEntity }) return@forEach //collision already handled by other party

            val speed = ((transform.position - transform.prevPosition)
                    - (otherTransform.position - otherTransform.prevPosition)).length()
            collider.collisions.add(ShipCollision(otherEntity, speed))
            otherCollider.collisions.add(ShipCollision(entity, speed))
        }
    }

}

internal inline fun <reified C1> Collection<Entity>.getComponents(): List<C1> =
    mapNotNull { it.getComponent<C1>() }

internal inline fun <reified C1> Collection<Entity>.withComponents(): Map<Entity, C1> =
    filter { it.hasComponents(C1::class.java) }
        .associateWith { it.getComponent<C1>()!! }

internal inline fun <reified C1, reified C2> Collection<Entity>.getComponents2(): List<Pair<C1, C2>> =
    filter { it.hasComponents(C1::class.java, C2::class.java) }
        .map { it.getComponent<C1>()!! to it.getComponent<C2>()!! }

internal inline fun <reified C1, reified C2> Collection<Entity>.withComponents2(): List<Triple<Entity, C1, C2>> =
    filter { it.hasComponents(C1::class.java, C2::class.java) }
        .map { Triple(it, it.getComponent<C1>()!!, it.getComponent<C2>()!!) }

internal inline fun <reified C1, reified C2, reified C3> Collection<Entity>.getComponents3(): List<Triple<C1, C2, C3>> =
    filter { it.hasComponents(C1::class.java, C2::class.java, C3::class.java) }
        .map { Triple(it.getComponent<C1>()!!, it.getComponent<C2>()!!, it.getComponent<C3>()!!) }
