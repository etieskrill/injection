import Cloth.POINTS_DIM
import Cloth.clothPoints
import Cloth.mainSphere
import org.etieskrill.engine.application.App
import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.DirectionalLightComponent
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.getComponent
import org.etieskrill.engine.entity.service.impl.RenderService
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.data.DirectionalLight
import org.etieskrill.engine.graphics.gl.shader.impl.GridShader
import org.etieskrill.engine.graphics.gl.shader.impl.camera
import org.etieskrill.engine.graphics.gl.shader.impl.position
import org.etieskrill.engine.graphics.model.model
import org.etieskrill.engine.graphics.model.sphere
import org.etieskrill.engine.input.controller.CursorCameraController
import org.etieskrill.engine.input.controller.KeyCameraController
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.window
import org.joml.Vector3f
import org.joml.minus
import org.joml.minusAssign
import org.joml.plus
import org.joml.plusAssign
import org.joml.primitives.Planef
import org.joml.primitives.Spheref
import org.joml.times
import org.lwjgl.opengl.GL11C.*
import org.lwjgl.opengl.GL30C.glBindVertexArray
import org.lwjgl.opengl.GL30C.glGenVertexArrays
import kotlin.math.exp

fun main() = Cloth.run()

object Cloth : App(window {
    title = "Cloth"
    size = Window.WindowSize.LARGEST_FIT
    mode = Window.WindowMode.BORDERLESS
}) {

    val camera = PerspectiveCamera(window.currentSize).apply {
        setOrbit(true)
        setOrbitDistance(4f)
        setRotation(-20f, 0f, 0f)
    }

    val gridShader = GridShader()
    val dummyVAO = glGenVertexArrays()

    val mainSphere = entitySystem.createEntity()
        .withComponent(Transform())
        .withComponent(Drawable(model("sphere") { sphere(1f, 1000) }))
        .withComponent(StaticCollider(Spheref(0f, 0f, 0f, 1f)))

    val clothPoints: List<Entity>

    //not too terrible considering the very primitive approach with verlet and some guesstimated constraint forces
    //does break relatively quickly at 50*50+ points, but a cape or some such should not require much more than, say,
    //a 5*20 or so grid
    //a cloak might be challenging though, especially the behaviour at the bottom tips, might be approximated better by
    //weighing lowermost ring of nodes more, or something like that
    const val POINTS_DIM = 20 //50

    init {
        entitySystem.createEntity()
            .withComponent(
                DirectionalLightComponent(
                    DirectionalLight(
                        Vector3f(-1f),
                        Vector3f(0f),
                        Vector3f(1f),
                        Vector3f(1f)
                    ), null, null
                )
            )

        val points = mutableListOf<MutableList<Entity>>()
        val clothPointModel = model("clothPoint") { sphere(0.01f, 20) }
        for (y in 0..<POINTS_DIM) {
            for (x in 0..<POINTS_DIM) {
                val position = Vector3f(x.toFloat() / (POINTS_DIM / 2f) - 1, 1.5f, y.toFloat() / (POINTS_DIM / 2f) - 1)
                val entity = entitySystem.createEntity()
                    .withComponent(Transform().setPosition(position))
                    .withComponent(Drawable(clothPointModel))
                    .withComponent(DynamicCollider(Spheref(0f, 0f, 0f, 0.01f), position))
                points.getOrNull(y)?.add(entity)
                    ?: points.add(mutableListOf(entity))
            }
        }
        for (y in 0..<POINTS_DIM) {
            for (x in 0..<POINTS_DIM) {
                points[y][x].withComponent(
                    MeshNeighbours(
                        if (y > 0) points[(y - 1)][x] else null,
                        if (x > 0) points[y][x - 1] else null,
                        if (y < POINTS_DIM - 1) points[y + 1][x] else null,
                        if (x < POINTS_DIM - 1) points[y][x + 1] else null,
                    )
                )
            }
        }
        clothPoints = points.flatten()
        check(clothPoints.all { it.hasComponents(MeshNeighbours::class.java) }) {
            clothPoints.filter { !it.hasComponents(MeshNeighbours::class.java) }.joinToString("\n")
        }

        entitySystem.addServices(
            RenderService(renderer, camera, window.currentSize).apply {
                frameBuffer.clearColour.set(0.1f, 0.1f, 0.1f, 1f)
                blur(false)
            }
        )

        window.addKeyInputs(KeyCameraController(camera))
        window.addCursorInputs(CursorCameraController(camera))

        window.cursor.disable()
    }

    override fun render() {
        gridShader.position = Vector3f(0f)
        gridShader.camera = camera

        gridShader.start()
        glBindVertexArray(dummyVAO)
        glDisable(GL_CULL_FACE)

        glDrawArrays(GL_POINTS, 0, 1)

        glEnable(GL_CULL_FACE)
    }

    var time = 0.0

    override fun loop(delta: Double) {
        for (point in clothPoints) {
            val transform = point.getComponent<Transform>()!!
            val collider = point.getComponent<DynamicCollider>()!!

            val gravity = 5f
            val newPosition = (transform.position * 2f) - collider.prevPosition +
                    Vector3f(0f, -gravity, 0f) * (delta * delta).toFloat()

            collider.prevPosition.set(transform.position)
            transform.position.set(newPosition)
        }

        repeat(4) {
            update(delta.toFloat() / 4f)
        }

        time += delta
        if (time > 3.0) {
            time = 0.0
            for (y in 0..<POINTS_DIM) {
                for (x in 0..<POINTS_DIM) {
                    val position =
                        Vector3f(x.toFloat() / (POINTS_DIM / 2f) - 1, 1.5f, y.toFloat() / (POINTS_DIM / 2f) - 1)
                    clothPoints[y * POINTS_DIM + x].apply {
                        getComponent<Transform>()!!.position.set(position)
                        getComponent<DynamicCollider>()!!.prevPosition.set(position)
                    }
                }
            }
        }
    }
}

fun update(delta: Float) {
    for (point in clothPoints) {
        val transform = point.getComponent<Transform>()!!
        val collider = point.getComponent<DynamicCollider>()!!

        val otherTransform = mainSphere.getComponent<Transform>()!!
        val otherCollider = mainSphere.getComponent<StaticCollider>()!!

        val normal = transform.position - otherTransform.position
        val overlap = normal.length() - (collider.shape as Spheref).r - (otherCollider.shape as Spheref).r
        if (overlap > 0) continue

        transform.position.minusAssign(normal.normalize() * overlap)
    }

    for (point in clothPoints) {
        val position = point.getComponent<Transform>()!!.position
        val neighbours = point.getComponent<MeshNeighbours>()!!

        neighbours.up?.constrainMesh(delta, position)
        neighbours.left?.constrainMesh(delta, position)
        neighbours.down?.constrainMesh(delta, position)
        neighbours.right?.constrainMesh(delta, position)
    }
}

fun Entity.constrainMesh(delta: Float, otherPosition: Vector3f) {
    val position = getComponent<Transform>()!!.position
    val normal = position - otherPosition
    val distance = normal.length() - (2f / POINTS_DIM)

    if (distance < 0f) return

    val force = 50000f * (exp(distance) - 1) * delta * delta / 2f //haha magic constant goes brrr
    position.minusAssign(normal.normalize() * force)
    otherPosition.plusAssign(normal.normalize() * force)
}

open class StaticCollider protected constructor(val shape: Any) {
    constructor(sphere: Spheref) : this(sphere as Any)
    constructor(plane: Planef) : this(plane as Any)
}

class DynamicCollider private constructor(shape: Any, val prevPosition: Vector3f = Vector3f()) : StaticCollider(shape) {
    constructor(sphere: Spheref, prevPosition: Vector3f) : this(sphere as Any, Vector3f(prevPosition))
    constructor(plane: Planef, prevPosition: Vector3f) : this(plane as Any, Vector3f(prevPosition))
}

class MeshNeighbours(val up: Entity?, val left: Entity?, val down: Entity?, val right: Entity?)
