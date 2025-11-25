package io.github.etieskrill.games.sails

import io.github.etieskrill.games.sails.ShipStats.State
import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.getComponent
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.engine.entity.system.EntitySystem
import org.joml.Math.PI_OVER_2_f
import org.joml.Math.PI_f
import org.joml.Matrix2f
import org.joml.Vector2f
import org.joml.minus
import org.joml.plus
import org.joml.times
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

class ShipStats(
    val maxHealth: Int,
    currentHealth: Int = maxHealth,

    var state: State = State.ALIVE,

    val rammingDamageTaken: Float = 1f,
    val rammingDamageDealt: Float = 1f,

    val hardpoints: Map<Hardpoint, Weapon?> = mapOf()
) {
    var currentHealth: Int = currentHealth
        set(value) {
            field = value
            if (field < 0) state = State.DYING
        }

    var deathProgress: Float = 0.0f
        set(value) {
            field = value
            if (field >= 1) state = State.DEAD
        }

    enum class State {
        ALIVE, DYING, DEAD
    }
}

data class Hardpoint(
    val position: Vector2f,
    val angle: Float,
    val angleLimit: Float,
    val turnSpeed: Float
)

open class Weapon(
    val damage: Float,
    val muzzleVelocity: Float,
    val projectileDrag: Float,
    val projectileSize: Float,
    val range: Float,
    val reloadTime: Float,
    val inaccuracy: Float = 0f,
    var angle: Float = 0f,
    var currentReloadTime: Float = reloadTime
)

data class Projectile(
    val damage: Float,
    val position: Vector2f,
    val prevPosition: Vector2f,
    val size: Float,
    val range: Float,
    val firingShip: Entity,
    var travelDistance: Float = 0f
)

class ShipService(val entitySystem: EntitySystem) : Service {
    override fun canProcess(entity: Entity) = entity.hasComponents(ShipStats::class.java)

    override fun process(targetEntity: Entity, entities: List<Entity>, delta: Double) {
        val stats = targetEntity.getComponent<ShipStats>()!!

        when (stats.state) {
            State.ALIVE -> {} //really not liking the required exhaustiveness for statement whens
            State.DYING -> stats.deathProgress += delta.toFloat()
            State.DEAD -> entitySystem.removeEntity(targetEntity)
        }
    }
}

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
            //TODO "shearing factor": scale damage based on dot product of movement direction to collision normal vector
            // should help in reducing the disproportionately high damage when two ships scrape laterally against each other
            //TODO allow damage below some threshold to only be dealt every 0.5 or so seconds
            val damage =
                it.speed * it.speed * min(1f, it.otherEntity.getComponent<NavalTransform>()!!.mass / transform.mass)
            val rammingDamageDealt = it.otherEntity.getComponent<ShipStats>()!!.rammingDamageDealt
            stats.currentHealth -= (2.5f * stats.rammingDamageTaken * rammingDamageDealt * damage).toInt()
        }
    }
}

class WeaponService(val entitySystem: EntitySystem) : Service {

    override fun canProcess(entity: Entity) = entity.hasComponents(NavalTransform::class.java, ShipStats::class.java)

    override fun process(entity: Entity, entities: List<Entity>, delta: Double) {
        val transform = entity.getComponent<NavalTransform>()!!
        val stats = entity.getComponent<ShipStats>()!!

        val enemies = entities
            .filter { it.hasComponents(NavalTransform::class.java, EnemyShipController::class.java) }
            .associateWith { it.getComponent<NavalTransform>()!!.run { position to size } }

        stats.hardpoints.filter { it.value != null }
            .forEach { (hardpoint, weapon) ->
                if (weapon!!.currentReloadTime > 0) {
                    weapon.currentReloadTime -= delta.toFloat()
                    return@forEach
                }

                val hardpointPosition =
                    transform.position + (Matrix2f().rotation(transform.rotation) * hardpoint.position)
                val hardpointAngle = wrap(transform.rotation + hardpoint.angle)
                val hardpointMinAngle = wrap(hardpointAngle - hardpoint.angleLimit)
                val hardpointMaxAngle = wrap(hardpointAngle + hardpoint.angleLimit)

                val (targetEntity, targetTransform) = enemies.mapNotNull { (_, enemyTransform) ->
                    val angle = wrap((hardpointPosition - enemyTransform.first).run { atan2(y, x) } + PI_OVER_2_f)
                    if (hardpointMinAngle < angle && angle < hardpointMaxAngle) {
                        entity to Triple(enemyTransform.first, enemyTransform.second, angle)
                    } else {
                        null
                    }
                }
                    .filter { (transform.position - it.second.first).length() < weapon.range }
                    .minByOrNull { (transform.position - it.second.first).length() }
                    ?: return@forEach

                weapon.angle = 0f

                entitySystem.configureEntity {
                    +Projectile(
                        weapon.damage,
                        hardpointPosition,
                        hardpointPosition + Vector2f(
                            weapon.muzzleVelocity * cos(targetTransform.third - PI_OVER_2_f),
                            weapon.muzzleVelocity * sin(targetTransform.third - PI_OVER_2_f)
                        ),
                        weapon.projectileSize,
                        weapon.range,
                        entity
                    )
                }

                weapon.currentReloadTime = weapon.reloadTime
            }
    }

    private fun wrap(angle: Float): Float {
        val x = (angle / (2f * PI_f)) - 0.5f
        return 2 * PI_f * (x - floor(x) - 0.5f)
    }
}

class ProjectileService(val entitySystem: EntitySystem) : Service {
    override fun canProcess(entity: Entity) = entity.hasComponents(Projectile::class.java)

    override fun process(targetEntity: Entity, entities: List<Entity>, delta: Double) {
        val projectile = targetEntity.getComponent<Projectile>()!!

        val velocity = projectile.position - projectile.prevPosition
        val newPosition = projectile.position + velocity
        projectile.prevPosition.set(projectile.position)
        projectile.position.set(newPosition)

        projectile.travelDistance += velocity.length()

        if (projectile.travelDistance > projectile.range) entitySystem.removeEntity(targetEntity)

        //TODO tunneling
        entities.withComponents2<NavalTransform, ShipStats>()
            .forEach { (entity, transform, stats) ->
                if (projectile.firingShip == entity) return@forEach
                if (stats.state != State.ALIVE) return@forEach

                if ((transform.position - projectile.position).length() < (transform.size + projectile.size) * 0.5) {
                    stats.currentHealth -= projectile.damage.toInt()
                    entitySystem.removeEntity(targetEntity)
                }
            }
    }
}
