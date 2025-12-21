package io.github.etieskrill.games.sails

import io.github.etieskrill.games.sails.EnemyShipController.Behaviours.CircleBehaviour
import org.etieskrill.engine.application.App
import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.system.EntitySystem
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.window
import org.joml.Math
import org.joml.Math.toRadians
import org.joml.Vector2f
import org.joml.Vector4f

fun main() {
    Game.run()
}

object Game : App(window {
    mode = Window.WindowMode.BORDERLESS
    size = Window.WindowSize.FHD
    samples = 4
}) {
    init {
        renderer.setClearColour(Vector4f(25f, 100f, 200f, 255f).div(255f))
        val camera = OrthographicCamera(window.currentSize)

        fun hardpoint(x: Float, y: Float, angle: Float) = Hardpoint(
            position = Vector2f(x, y),
            angle = toRadians(angle),
            angleLimit = toRadians(15f),
            turnSpeed = 1f
        )

        class Cannon : Weapon(
            damage = 10f,
            muzzleVelocity = 10f,
            projectileDrag = 0f,
            projectileSize = 10f,
            range = 500f,
            reloadTime = 5f
        )

        entitySystem.configureEntity {
            +NavalTransform(position = Vector2f(0f, -300f))
            +InputDirection()
            +PlayerShipController()
            +ShipCollider()
            +ShipStats(
                100, faction = PLAYER_FACTION, hardpoints = mapOf(
                    hardpoint(-20f, 10f, 90f) to Cannon(),
                    hardpoint(-20f, -10f, 90f) to Cannon(),
                    hardpoint(-20f, -30f, 90f) to Cannon(),
                    hardpoint(20f, 10f, -90f) to Cannon(),
                    hardpoint(20f, -10f, -90f) to Cannon(),
                    hardpoint(20f, -30f, -90f) to Cannon()
                )
            )
        }

        for (position in listOf<Vector2f>(
//            Vector2f(-400f, 300f),
//            Vector2f(-200f, 300f),
            Vector2f(0f, 300f),
//            Vector2f(200f, 300f),
//            Vector2f(400f, 300f)
        )) {
            entitySystem.configureEntity {
                +NavalTransform(position, rotation = Math.PI_f, size = 80f, mass = 70f)
                +InputDirection()
                +EnemyShipController(CircleBehaviour(400f, 700f))
                +ShipCollider()
                +ShipStats(
                    50, faction = ENEMY_FACTION, hardpoints = mapOf(
                        hardpoint(-15f, -10f, 90f) to Cannon(),
                        hardpoint(15f, -10f, -90f) to Cannon()
                    )
                )
            }
        }

        entitySystem.addServices(
            PlayerShipControllerService(window),
            EnemyShipControllerService(),
            ShipService(entitySystem),
            ShipPhysicsService(),
            ShipCollisionService(),
            WeaponService(entitySystem),
            ProjectileService(entitySystem),
            ShipRenderService(camera, renderer, renderer, window),
            ProjectileRenderService(camera, renderer, window)
        )
    }

    override fun loop(delta: Double) {
    }
}

class EntityBuilder(val entity: Entity) {
    operator fun Any.unaryPlus() = entity.withComponent(this)!!
}

fun EntitySystem.configureEntity(block: EntityBuilder.() -> Unit) = EntityBuilder(createEntity()).apply(block).entity
