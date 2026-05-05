package io.github.etieskrill.games.circles

import org.etieskrill.engine.application.App
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.impl.DeferredRenderService
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.gl.StorageBufferObject
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.etieskrill.engine.graphics.gl.shader.impl.BlitShader
import org.etieskrill.engine.graphics.gl.shader.impl.SolidShader
import org.etieskrill.engine.graphics.model.box
import org.etieskrill.engine.graphics.model.model
import org.etieskrill.engine.graphics.model.plane
import org.etieskrill.engine.graphics.model.sphere
import org.etieskrill.engine.graphics.pipeline.PostPassPipeline
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.input.controller.CursorCameraController
import org.etieskrill.engine.window.Window
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4f
import org.joml.minus
import kotlin.math.sqrt

fun main() {
    Main().run()
}

class Main : App(
    Window(
        title = "Circles",
        size = Window.WindowSize.FHD,
        mode = Window.WindowMode.BORDERLESS
    )
) {

    val sdfBuffer = StorageBufferObject(100, SDFShader.SDFVertexAccessor)
    val sdfFrameBuffer = FrameBuffer.getColour(Vector2i(800))!!
    val pipeline = PostPassPipeline(SDFShader(), sdfFrameBuffer)

    val screenPipeline = PostPassPipeline(BlitShader(), window.screenBuffer, opaque = false)

    val primRuneFire = EmitterRune(
        "fire", 10f, mapOf(VisType("fire", Vector4f(1f, 0.5f, 0f, 1f)) to 1f)
    ) { pos, dir, placedOn ->
        placedOn.getComponent<Heatable>()?.apply {
            energy += 100
            return@EmitterRune
        }

        placedOn.getComponent<Environment>()
            ?.apply { //FIXME no idea if this is agreeable with env entity/entities
                addHeatEnergy(pos, 100f)
                return@EmitterRune
            }
    }
    val auxRuneGebo = AuxiliaryRune("gebo")
    val auxRuneNauthiz = AuxiliaryRune("nauthiz")
    val heatingCircle = Circle(
        placedOn = entitySystem.createEntity {}, visCapacity = 100f,
        streamUpkeepAbsolute = 1f, streamUpkeepRelative = 0.2f,
        focalRune = primRuneFire, runes = listOf(), numRings = 1,
        auxRunes = listOf(
            listOf(listOf(auxRuneGebo, auxRuneNauthiz, auxRuneGebo, auxRuneNauthiz, auxRuneGebo, auxRuneNauthiz))
        )
    )

    val camera = PerspectiveCamera(window.size)
        .apply { setOrbit(true); setOrbitDistance(5f); setRotation(-45f, 45f, 0f) }
//               orbit = true, orbitDistance = 5f, rotation = Vec3(-45f, 45f, 0f).eulerDeg

    val shader = SolidShader()

    init {
        entitySystem.createEntity {
            +Transform()
            +heatingCircle
        }

        entitySystem.addServices(
            CircleService(object : VisEnvironment {
                override fun requestVis(id: Int, type: VisType, position: Vector3fc, strength: Float, max: Float) = Unit
                override fun getVis(id: Int, type: VisType) = 5f
                override fun update(delta: Float) = Unit
            }),
            HeatService(object : Environment {
                override fun getTemperature(position: Vector3fc) = 10f
                override fun addHeatEnergy(position: Vector3fc, energy: Float) = Unit
                override fun update(delta: Float) = Unit
            }),
            DeferredRenderService(renderer, window.screenBuffer, camera)
        )

        entitySystem.createEntity {
            +Transform()
            +Drawable(model("environment") {
                val d = sqrt(2f) / 2f
                sphere(radius = 1f, numPoints = 100, transform = Transform().setPosition(Vector3f(d, 0f, d)))
                box(Vector3f(0f), Vector3f(1f), Transform().setPosition(Vector3f(-1f, 0f, -1f)))
                plane(a = Vector2f(-3f), b = Vector2f(3f))
                //FIXME culling's fucked methinks
            }, shader.shader as ShaderProgram)
        }

        window.cursorInputs += CursorCameraController(camera)
        window.cursor.disable()
    }

    override fun loop(delta: Double) {
        shader.viewPosition = camera.viewPosition
    }

    override fun render() {
        sdfFrameBuffer.clear()
        renderCircle(heatingCircle, Vector2f(0f), 0.5f, renderer)

        screenPipeline.shader.apply {
            sprite = sdfFrameBuffer.attachments[FrameBufferAttachment.BufferAttachmentType.COLOUR0] as Texture2D
            useSpriteColour = true
            position = Vector2f(window.size) - Vector2f(200f)
            size = Vector2f(200f)
            windowSize = Vector2f(window.size)
        }
        renderer.render(screenPipeline)
    }

}
