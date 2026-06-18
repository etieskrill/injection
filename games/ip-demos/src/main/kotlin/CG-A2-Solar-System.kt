import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.ShaderVertexData
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.application.App
import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.impl.RenderService
import org.etieskrill.engine.graphics.camera.Camera
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.gl.VertexArrayAccessor
import org.etieskrill.engine.graphics.gl.VertexArrayObject
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.gl.renderer.GLRenderer
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.etieskrill.engine.graphics.gl.shader.impl.SingleColourShader
import org.etieskrill.engine.graphics.gl.shader.impl.colour
import org.etieskrill.engine.graphics.model.CubeMapModel
import org.etieskrill.engine.graphics.model.model
import org.etieskrill.engine.graphics.model.sphere
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PipelineConfig
import org.etieskrill.engine.graphics.pipeline.PrimitiveType
import org.etieskrill.engine.input.controller.CursorCameraController
import org.etieskrill.engine.util.FixedArrayDeque
import org.etieskrill.engine.window.Window
import org.joml.Vector2ic
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4f
import org.lwjgl.opengl.GL11C.glLineWidth
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    `CG-A2-Solar-System`().run()
}

class `CG-A2-Solar-System` : App(
    Window(
    size = Window.WindowSize.SVGA
    )
) {
    init {
        val shader = SingleColourShader()
        shader.colour = Vector4f(1f, 1f, 0f, 1f)

        val sphereModel = model("sphere") { sphere(0.1f, 400) }

        val sun = Planet(
            size = 15f,
            colour = Vector4f(1f, 1f, 0f, 1f),
            rotationSpeed = 10f,
            orbitDistance = 0f,
            orbitSpeed = 0f
        )
        val vole = Planet(
            size = 4f,
            colour = Vector4f(1f, 0.318f, 0f, 1f),
            rotationSpeed = 50f,
            orbitDistance = 3f,
            orbitSpeed = 0.5f,
            parent = sun
        )
        val io = Planet(
            size = 1f,
            colour = Vector4f(0.2f, 0f, 1f, 1f),
            rotationSpeed = 100f,
            orbitDistance = 1f,
            orbitSpeed = 2f,
            parent = vole
        )
        val callisto = Planet(
            size = 2f,
            colour = Vector4f(0f, 0.5f, 1f, 1f),
            rotationSpeed = 25f,
            orbitDistance = 6f,
            orbitSpeed = 0.2f,
            parent = sun
        )
        val planets = listOf(sun, vole, io, callisto)

        for (planet in planets) {
            entitySystem.createEntity {
                +Transform()
                +Drawable(sphereModel, shader).apply { isWireframeEnabled = true }
                +planet
            }
        }

        val camera = PerspectiveCamera(window.size).apply {
            setOrbit(true)
            setOrbitDistance(15f)
            setRotation(-30f, 0f, 0f)
        }

        entitySystem.addService(PlanetService(window.screenBuffer, renderer, camera, window.size).apply {
            skybox = CubeMapModel("textures/cubemaps/space")
            blur(false)
        })

        window.cursorInputs += CursorCameraController(camera)

        window.cursor.disable()
    }

    override fun loop(delta: Double) {}
}

data class Planet(
    val size: Float,
    val colour: Vector4f,
    val rotationSpeed: Float,
    val orbitDistance: Float,
    val orbitSpeed: Float,
    //TODO orbital inclination etc.

    val parent: Planet? = null,

    var rotation: Float = 0f,
    var orbitAngle: Float = 0f,

    internal val previousPositions: FixedArrayDeque<Vector3fc> = FixedArrayDeque(1000),
    internal val trailVAO: VertexArrayObject<Vector3fc> = VertexArrayObject(Vector3fcAccessor, numVertexElements = 1000),
    internal var trailPipeline: Pipeline<TrailShader>? = null
)

private object Vector3fcAccessor : VertexArrayAccessor<Vector3fc>() {
    override fun registerFields() {
        addField<Vector3fc> { vec3, buffer -> vec3.get(buffer) }
    }
}

open class PlanetService(screenBuffer: FrameBuffer, renderer: GLRenderer, camera: Camera, windowSize: Vector2ic) :
    RenderService(screenBuffer, renderer, camera, windowSize) {

    private val trailShader = TrailShader()

    override fun canProcess(entity: Entity): Boolean {
        return super.canProcess(entity) && entity.hasComponents<Planet>()
    }

    override fun process(targetEntity: Entity, entities: List<Entity>, delta: Double) {
        if (targetEntity.getComponent<Boolean>() == false) return

        val drawable = targetEntity.getComponent<Drawable>()!!
        if (!drawable.isVisible) return

        val transform = targetEntity.getComponent<Transform>()!!

        val planet = targetEntity.getComponent<Planet>()!!
        planet.rotation += planet.rotationSpeed * delta.toFloat()

        transform.scale.set(planet.size)
        transform.rotation.fromAxisAngleDeg(Vector3f(0f, 1f, 0f), planet.rotation)

        planet.orbitAngle += planet.orbitSpeed * delta.toFloat()
        val orbitPosition = Vector3f(cos(planet.orbitAngle), 0f, sin(planet.orbitAngle)).mul(planet.orbitDistance)

        var parent = planet.parent
        while (parent != null) {
            orbitPosition.add(
                cos(parent.orbitAngle) * parent.orbitDistance,
                0f,
                sin(parent.orbitAngle) * parent.orbitDistance
            )
            parent = parent.parent
        }
        transform.position = orbitPosition

        if (!planet.previousPositions.isFull) planet.previousPositions.fill(orbitPosition)
        planet.previousPositions += orbitPosition
        planet.trailVAO.vertices = planet.previousPositions

        if (planet.trailPipeline == null) {
            planet.trailPipeline = Pipeline(
                planet.trailVAO,
                PipelineConfig(
                    primitiveType = PrimitiveType.LINE_STRIP,
                    lineWidth = planet.size.let { x -> -2 / (x + 1) + 3 }
                ),
                trailShader,
                frameBuffer
            )
        }

        trailShader.colour = planet.colour
        trailShader.combined = camera.combined

        renderer.render(planet.trailPipeline)

        frameBuffer.bind()
        val shader = getConfiguredShader(targetEntity, drawable)
        shader.setUniform("colour", planet.colour, false)
        glLineWidth(1f)
        if (!drawable.isWireframeEnabled) {
            renderer.render(transform, drawable.model, shader, camera)
        } else {
            renderer.renderWireframe(transform, drawable.model, shader, camera)
        }
    }

}

class TrailShader : ShaderBuilder<TrailShader.VertexAttributes, TrailShader.Vertex, ColourRenderTarget>(
    object : ShaderProgram(listOf("Trail.glsl")) {}
) {
    data class VertexAttributes(val position: vec3)
    data class Vertex(override val position: vec4) : ShaderVertexData

    var colour by uniform<vec4>()

    var combined by uniform<mat4>()

    override fun program() {
        vertex { Vertex(combined * vec4(it.position, 1.0)) }
        fragment { ColourRenderTarget(colour) }
    }
}
