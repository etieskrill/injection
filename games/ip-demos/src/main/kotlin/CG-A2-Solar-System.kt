import org.etieskrill.engine.application.GameApplication
import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.impl.RenderService
import org.etieskrill.engine.graphics.camera.Camera
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.gl.renderer.GLRenderer
import org.etieskrill.engine.graphics.gl.shader.impl.SingleColourShader
import org.etieskrill.engine.graphics.gl.shader.impl.colour
import org.etieskrill.engine.graphics.model.CubeMapModel
import org.etieskrill.engine.graphics.model.model
import org.etieskrill.engine.graphics.model.sphere
import org.etieskrill.engine.input.controller.CursorCameraController
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.window
import org.joml.Vector2ic
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    `CG-A2-Solar-System`().run()
}

class `CG-A2-Solar-System` : GameApplication(window {
    size = Window.WindowSize.SVGA
}) {
    val camera = PerspectiveCamera(window.currentSize).apply {
        setOrbit(true)
        setOrbitDistance(15f)
        setRotation(-30f, 0f, 0f)
    }

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
            orbitSpeed = 0.2f
        )
        val planets = listOf(sun, vole, io, callisto)

        for (planet in planets) {
            entitySystem.createEntity()
                .withComponent(Transform())
                .withComponent(Drawable(sphereModel, shader).apply { isDrawWireframe = true })
                .withComponent(planet)
        }

        entitySystem.addService(PlanetService(renderer, camera, window.currentSize).apply {
            this.skybox = CubeMapModel("space")
        })

        window.addCursorInputs(CursorCameraController(camera))

        window.cursor.disable()
    }

    override fun loop(delta: Double) {
//        renderer.render(skybox, skyboxShader.shader as ShaderProgram, camera.combined)
    }
}

data class Planet(
    val size: Float,
    val colour: Vector4f,
    val rotationSpeed: Float,
    val orbitDistance: Float,
    val orbitSpeed: Float,

    val parent: Planet? = null,

    var rotation: Float = 0f,
    var orbitAngle: Float = 0f
)

open class PlanetService(
    renderer: GLRenderer,
    val camera: Camera,
    windowSize: Vector2ic
) : RenderService(renderer, camera, windowSize) {
    override fun canProcess(entity: Entity): Boolean {
        return super.canProcess(entity) && entity.hasComponents(Planet::class.java)
    }

    override fun process(targetEntity: Entity, entities: List<Entity?>, delta: Double) {
        val enabled = targetEntity.getComponent(Boolean::class.java)
        if (enabled != null && !enabled) return

        val drawable = targetEntity.getComponent(Drawable::class.java)!!
        if (!drawable.isVisible) return

        val transform = targetEntity.getComponent(Transform::class.java)!!

        val planet = targetEntity.getComponent(Planet::class.java)!!
        planet.rotation += planet.rotationSpeed * delta.toFloat()

        transform.scale.set(planet.size)
        transform.applyRotation { it.fromAxisAngleDeg(Vector3f(0f, 1f, 0f), planet.rotation) }

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
        transform.setPosition(orbitPosition)

        val shader = getConfiguredShader(targetEntity, drawable)
        shader.setUniform("colour", planet.colour, false)
        if (!drawable.isDrawWireframe) {
            renderer.render(transform, drawable.model, shader, camera)
        } else {
            renderer.renderWireframe(transform, drawable.model, shader, camera)
        }
    }
}
