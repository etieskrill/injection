import org.etieskrill.engine.application.App
import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.getComponent
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.engine.graphics.Renderer
import org.etieskrill.engine.graphics.camera.Camera
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.data.DirectionalLight
import org.etieskrill.engine.graphics.gl.shader.impl.StaticShader
import org.etieskrill.engine.graphics.gl.shader.impl.globalLights
import org.etieskrill.engine.graphics.model.model
import org.etieskrill.engine.graphics.model.sphere
import org.etieskrill.engine.input.controller.CursorCameraController
import org.etieskrill.engine.input.controller.KeyCameraController
import org.etieskrill.engine.time.LoopPacer
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.window
import org.joml.Vector3f
import org.joml.minus
import org.joml.minusAssign
import org.joml.plus
import org.joml.plusAssign
import org.joml.times
import org.lwjgl.stb.STBPerlin.stb_perlin_noise3
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min

val height = 20
val width = 8

data class ClothPoint(
    val position: Vector3f,
    val fixed: Boolean,
    var up: ClothPoint? = null,
    var left: ClothPoint? = null,
    var down: ClothPoint? = null,
    var right: ClothPoint? = null,
    val prevPosition: Vector3f = Vector3f(position),
    val initialPosition: Vector3f = Vector3f(position)
)

data class ClothCollider(val points: List<ClothPoint>)

class ClothPhysicsService(val pacer: LoopPacer) : Service {
    override fun canProcess(entity: Entity) = entity.hasComponents(ClothCollider::class.java)

    override fun process(targetEntity: Entity, entities: List<Entity?>, delta: Double) {
        val points = targetEntity.getComponent<ClothCollider>()!!.points

        for (point in points) {
            if (point.fixed) continue

            val gravity = Vector3f(0f, -10f, 0f)

            val noise = stb_perlin_noise3(
                pacer.time.toFloat() + point.position.x,
                pacer.time.toFloat() + point.position.y,
                0f, 0, 0, 0
            )
            val windAmp = 2 * (stb_perlin_noise3(
                5f * pacer.time.toFloat() + point.position.x,
                5f * pacer.time.toFloat() + point.position.y,
                0f, 0, 0, 0
            ) + 1f)
            val windStr = (noise + 1f) / 2f
            val dir = noise / 2f
            val wind = Vector3f(0.5f + dir, 0f, 0.5f + (1f - dir)).normalize() * (windStr * windAmp * windAmp)

            val newPosition = point.position + (point.position - point.prevPosition) * 0.99f +
                    (gravity + wind) * (delta.toFloat() * delta.toFloat())
            point.prevPosition.set(point.position)
            point.position.set(newPosition)
        }

        repeat(8) {
            update(points, delta.toFloat())
        }
    }

    fun update(points: List<ClothPoint>, delta: Float) {
        for (point in points) {
            point.apply {
                up?.constrainMesh(delta, position)
                left?.constrainMesh(delta, position)
                down?.constrainMesh(delta, position)
                right?.constrainMesh(delta, position)
            }
            if (point.fixed) point.position.set(point.initialPosition)
        }
    }

    fun ClothPoint.constrainMesh(delta: Float, otherPosition: Vector3f) {
        val normal = position - otherPosition
        val distance = normal.length() - (1f / (height / 2f))

        if (distance < 0f) return

        var force = 1000f * (exp(distance) - 1) * delta * delta / 2f //haha magic constant goes brrr
        force = min(force, 1f)
        position.minusAssign(normal.normalize() * force)
        otherPosition.plusAssign(normal.normalize() * force)
    }
}

class ClothRenderService(val renderer: Renderer, val camera: Camera) : Service {
    val pointModel = model("clothPoint") { sphere(0.01f, 20) }
    val shader = StaticShader()

    override fun canProcess(entity: Entity) = entity.hasComponents(ClothCollider::class.java)

    override fun process(targetEntity: Entity, entities: List<Entity?>, delta: Double) {
        val transform = targetEntity.getComponent<Transform>()!!
        val clothCollider = targetEntity.getComponent<ClothCollider>()!!

        val pointTransform = Transform()
        for (point in clothCollider.points) {
            pointTransform.set(transform)
                .translate(point.position)

            shader.globalLights = arrayOf(
                DirectionalLight(
                    Vector3f(-1f),
                    Vector3f(0.05f),
                    Vector3f(1f),
                    Vector3f(1f),
                )
            )

            renderer.render(pointTransform, pointModel, shader, camera)
        }
    }
}

object Cape : App(window {
    size = Window.WindowSize.LARGEST_FIT
    mode = Window.WindowMode.BORDERLESS
}) {
    init {
        val points = mutableListOf<MutableList<ClothPoint>>()
        for (y in 0..height) {
            for (x in 0..width) {
                val position = Vector3f((x - (width / 2f)) / (height / 2f), y / (height / 2f) - 1f, 0f)
                val point = ClothPoint(position, y == height && (abs(x.toFloat() - width / 2f) > 2f))
                points.getOrNull(y)?.add(point)
                    ?: points.add(mutableListOf(point))
            }
        }
        for (y in 0..height) {
            for (x in 0..width) {
                points[y][x].apply {
                    up = if (y > 0) points[y - 1][x] else null
                    left = if (x > 0) points[y][x - 1] else null
                    down = if (y > 0) points[y - 1][x] else null
                    right = if (x > 0) points[y][x - 1] else null
                }
            }
        }

        entitySystem.createEntity()
            .withComponent(Transform())
            .withComponent(ClothCollider(points.flatten()))

        val camera = PerspectiveCamera(window.currentSize).apply {
            setOrbit(true)
            setOrbitDistance(3f)
        }
        entitySystem.addServices(ClothPhysicsService(pacer), ClothRenderService(renderer, camera))

        //TODO
        // - find cloak model, potentially rigged
        // - map to points / adjust points if not convenient
        // - profit
        // - find out about standards for cloak rigs, if any

        window.addKeyInputs(KeyCameraController(camera))
        window.addCursorInputs(CursorCameraController(camera))
        window.cursor.disable()
    }

    override fun loop(delta: Double) {}
}

fun main() = Cape.run()
