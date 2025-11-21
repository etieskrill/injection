package io.github.etieskrill.games.sails

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.vec2
import org.etieskrill.engine.application.App
import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.system.EntitySystem
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.etieskrill.engine.input.KeyInputManager
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.window
import org.joml.Math
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector4f
import kotlin.math.floor

fun main() {
    Game.run()
}

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

data class InputDirection(val direction: Vector2f, var strength: Float) {
    constructor() : this(Vector2f(), 0f)
}

class PlayerShipController(var initialised: Boolean = false) : KeyInputManager()
class EnemyShipController

data class ShipCollision(val entity: Entity, val speed: Float) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is ShipCollision) return false
        return entity == other.entity
    }

    override fun hashCode() = entity.hashCode()

    override fun toString() = "ShipCollision(entity=${entity.id}, speed=$speed)"
}

data class ShipCollider(
    //TODO custom collider shape
    val collisions: MutableSet<ShipCollision> = mutableSetOf()
)

class ShipStats(
    val maxHealth: Int,
    currentHealth: Int = maxHealth,

    var state: State = State.ALIVE,

    val rammingDamageModifier: Float = 0.5f,

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
    val projectileVelocity: Float,
    val range: Float,
    val angle: Float,
)

object Game : App(window {
    mode = Window.WindowMode.BORDERLESS
    size = Window.WindowSize.FHD
    samples = 4
}) {
    val playerShip: Entity

    init {
        renderer.setClearColour(Vector4f(25f, 100f, 200f, 255f).div(255f))

        playerShip = entitySystem.configureEntity {
            +NavalTransform(
                position = Vector2f(window.currentSize.div(2f, Vector2i())),
                rotation = Math.PI_f
            )
            +InputDirection()
            +PlayerShipController()
            +ShipCollider()
            +ShipStats(
                100, hardpoints = mapOf(
                    Hardpoint(Vector2f(0.04f, -0.03f + 0.02f), 90f, 15f, 1f) to null,
                    Hardpoint(Vector2f(0.04f, 0f + 0.02f), 90f, 15f, 1f) to null,
                    Hardpoint(Vector2f(0.04f, 0.03f + 0.02f), 90f, 15f, 1f) to null,
                    Hardpoint(Vector2f(-0.04f, -0.03f + 0.02f), -90f, 15f, 1f) to null,
                    Hardpoint(Vector2f(-0.04f, 0f + 0.02f), -90f, 15f, 1f) to null,
                    Hardpoint(Vector2f(-0.04f, 0.03f + 0.02f), -90f, 15f, 1f) to null,
                )
            )
        }

        entitySystem.configureEntity {
            +NavalTransform(
                Vector2f(window.currentSize.div(4f, Vector2i()).mul(2, 1)),
                size = 80f,
                mass = 70f
            )
            +InputDirection()
            +EnemyShipController()
            +ShipCollider()
            +ShipStats(50)
        }

        entitySystem.configureEntity {
            +NavalTransform(
                Vector2f(window.currentSize.div(4f, Vector2i()).mul(2, 1)).sub(100f, 0f),
                size = 80f,
                mass = 70f
            )
            +InputDirection()
            +EnemyShipController()
            +ShipCollider()
            +ShipStats(50)
        }

        entitySystem.configureEntity {
            +NavalTransform(
                Vector2f(window.currentSize.div(4f, Vector2i()).mul(2, 1)).add(100f, 0f),
                size = 80f,
                mass = 70f
            )
            +InputDirection()
            +EnemyShipController()
            +ShipCollider()
            +ShipStats(50)
        }

        entitySystem.addServices(
            PlayerShipControllerService(window),
            EnemyShipControllerService(),
            ShipPhysicsService(),
            ShipCollisionService(),
            ShipRenderService(renderer, renderer, window)
        )
    }

//    private val islandsPipeline = PostPassPipeline(IslandShader(), null, opaque = false, depthTest = false)

    override fun loop(delta: Double) {
//        islandsPipeline.shader.apply {
//            position = playerShip.getComponent<NavalTransform>()!!.position
//            screenSize = Vector2f(window.currentSize)
//        }
//        renderer.render(islandsPipeline)
    }
}

class EntityBuilder(val entity: Entity) {
    operator fun Any.unaryPlus() = entity.withComponent(this)!!
}

fun EntitySystem.configureEntity(block: EntityBuilder.() -> Unit) = EntityBuilder(createEntity()).apply(block).entity

class IslandShader : PureShaderBuilder<VertexData, ColourRenderTarget>(
    object : ShaderProgram(listOf("Island.glsl"), false) {}
) {
    val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    var position by uniform<vec2>()
    var screenSize by uniform<vec2>()

    private val p by const(
        intArrayOf( //TODO constant values should be initialisable without the delegate
            151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225,
            140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23, 190, 6, 148,
            247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32,
            57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175,
            74, 165, 71, 134, 139, 48, 27, 166, 77, 146, 158, 231, 83, 111, 229, 122,
            60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102, 143, 54,
            65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169,
            200, 196, 135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64,
            52, 217, 226, 250, 124, 123, 5, 202, 38, 147, 118, 126, 255, 82, 85, 212,
            207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183, 170, 213,
            119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9,
            129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224, 232, 178, 185, 112, 104,
            218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241,
            81, 51, 145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157,
            184, 84, 204, 176, 115, 121, 50, 45, 127, 4, 150, 254, 138, 236, 205, 93,
            222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180,
            151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225,
            140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23, 190, 6, 148,
            247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32,
            57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175,
            74, 165, 71, 134, 139, 48, 27, 166, 77, 146, 158, 231, 83, 111, 229, 122,
            60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102, 143, 54,
            65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169,
            200, 196, 135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64,
            52, 217, 226, 250, 124, 123, 5, 202, 38, 147, 118, 126, 255, 82, 85, 212,
            207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183, 170, 213,
            119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9,
            129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224, 232, 178, 185, 112, 104,
            218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241,
            81, 51, 145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157,
            184, 84, 204, 176, 115, 121, 50, 45, 127, 4, 150, 254, 138, 236, 205, 93,
            222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180
        )
    )

    private fun fade(t: Double) =
        func { t * t * t * (t * (t * 6 - 15) + 10) } //TODO auto-order functions or declare prototypes

    private fun lerp(t: Double, a: Double, b: Double) = func { a + t * (b - a) }

    private fun grad(hash: Int, x: Double, y: Double, z: Double) = func {
        // Convert low 4 bits of hash code into 12 gradient directions
        val h = hash and 15
        val u = if (h < 8) x else y
        val v = if (h < 4) y else if (h == 12 || h == 14) x else z
//        return@func (if ((h and 1) == 0) u else -u) + //FIXME cmon man
//                (if ((h and 2) == 0) v else -v)
        val grad1 = if ((h and 1) == 0) u else -u
        val grad2 = if ((h and 2) == 0) v else -v
        return@func grad1 + grad2
    }

    fun noise(x: Double, y: Double, z: Double) = func {
        // Find unit cube that contains point
        val xi = floor(x).toInt() and 255
        val yi = floor(y).toInt() and 255
        val zi = floor(z).toInt() and 255

        // Find relative x, y, z of point in cube
        val xx = x - floor(x)
        val yy = y - floor(y)
        val zz = z - floor(z)

        // Compute fade curves for each of xx, yy, zz
        val u = fade(xx)
        val v = fade(yy)
        val w = fade(zz)

        // Hash co-ordinates of the 8 cube corners
        // and add blended results from 8 corners of cube

        val a = p[xi] + yi
        val aa = p[a] + zi
        val ab = p[a + 1] + zi
        val b = p[xi + 1] + yi
        val ba = p[b] + zi
        val bb = p[b + 1] + zi

        return@func lerp(
            w, lerp(
                v, lerp(
                    u, grad(p[aa], xx, yy, zz),
                    grad(p[ba], xx - 1, yy, zz)
                ),
                lerp(
                    u, grad(p[ab], xx, yy - 1, zz),
                    grad(p[bb], xx - 1, yy - 1, zz)
                )
            ),
            lerp(
                v, lerp(
                    u, grad(p[aa + 1], xx, yy, zz - 1),
                    grad(p[ba + 1], xx - 1, yy, zz - 1)
                ),
                lerp(
                    u, grad(p[ab + 1], xx, yy - 1, zz - 1),
                    grad(p[bb + 1], xx - 1, yy - 1, zz - 1)
                )
            )
        )
    }

    fun noise(x: Double, y: Double) = func { noise(x, y, 0.0) }
    fun noise(x: Double) = func { noise(x, 0.0, 0.0) }

    override fun program() {
        vertex { VertexData(vec4(vertices[vertexID], 0, 1)) }
        fragment {
//            val pos = (position * vec2(1.618 * (screenSize.x / screenSize.y), 1.618)) / screenSize
//            val pos = (3.141 * 2 * position) / screenSize
            val pos = position / screenSize
            var noise = noise(
                -pos.x.toDouble() + it.position.x.toDouble() * (screenSize.x.toDouble()),
                pos.y.toDouble() + it.position.y.toDouble() * (screenSize.y.toDouble())
            )
            val x =
                if ((-pos.x.toDouble() + it.position.x.toDouble() * (screenSize.x.toDouble() / 500.0)) % 0.5 > 0.25) 1 else 0
            val y =
                if ((pos.y.toDouble() / (screenSize.x.toDouble() / screenSize.y.toDouble()) + it.position.y.toDouble() * (screenSize.y.toDouble() / 500.0)) % 0.5 > 0.25) 1 else 0
            val col = if (x == 1 && y == 0 || x == 0 && y == 1) 1 else 0
            noise += -noise + col
            ColourRenderTarget(vec4(noise, noise, noise, 1).rt)
        }
    }
}
