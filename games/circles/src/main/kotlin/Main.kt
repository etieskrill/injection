package io.github.etieskrill.games.circles

import org.etieskrill.engine.application.App
import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.getComponent
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.graphics.gl.StorageBufferObject
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment
import org.etieskrill.engine.graphics.gl.shader.Shaders
import org.etieskrill.engine.graphics.gl.shader.impl.BlitShader
import org.etieskrill.engine.graphics.pipeline.PostPassPipeline
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.window
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3fc
import org.joml.Vector4f
import org.joml.minus

fun main() {
    Main().run()
}

class Main : App(
    window {
        title = "Circles"
        size = Window.WindowSize.FHD
        mode = Window.WindowMode.BORDERLESS
        position = Vector2f(1000f, 200f)
    }
) {

    val camera = OrthographicCamera(window.currentSize).apply { rotate(0f, 180f, 0f) }

    val sdfBuffer = StorageBufferObject(100, SDFShader.SDFVertexAccessor)
    val sdfFrameBuffer = FrameBuffer.getColour(Vector2i(800))!!
    val pipeline = PostPassPipeline(SDFShader(), sdfFrameBuffer)

    val screenPipeline = PostPassPipeline(BlitShader(), screenBuffer, opaque = false)

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
        placedOn = Entity(0), visCapacity = 100f, streamUpkeepAbsolute = 1f, streamUpkeepRelative = 0.2f,
        focalRune = primRuneFire, runes = listOf(), numRings = 1,
        auxRunes = listOf(
            listOf(listOf(auxRuneGebo, auxRuneNauthiz, auxRuneGebo, auxRuneNauthiz, auxRuneGebo, auxRuneNauthiz))
        )
    )

    init {
        entitySystem.createEntity()
            .withComponent(Transform())
            .withComponent(heatingCircle)

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
            })
        )

        screenBuffer.setClearColour(Vector4f(0.25f, 0.25f, 0.25f, 1f))
    }

    override fun loop(delta: Double) {}

    override fun render() {
        sdfFrameBuffer.clear()
        renderCircle(heatingCircle, Vector2f(0f), 0.5f, renderer)

        screenPipeline.shader.apply {
            sprite = sdfFrameBuffer.attachments[FrameBufferAttachment.BufferAttachmentType.COLOUR0] as Texture2D
            useSpriteColour = true
            position = Vector2f(window.currentSize) - Vector2f(200f)
            size = Vector2f(200f)
            windowSize = Vector2f(window.currentSize)
        }
        renderer.render(screenPipeline)
    }

}
