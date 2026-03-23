package io.github.etieskrill.games.circles

import org.etieskrill.engine.application.App
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.graphics.gl.StorageBufferObject
import org.etieskrill.engine.graphics.pipeline.PostPassPipeline
import org.etieskrill.engine.input.CursorInputAdapter
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.window
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector4f
import org.joml.Vector4fc

fun main() {
    Main().run()
}

class Main : App(
    window {
        title = "Circles"
        size = Window.WindowSize.XGA
        mode = Window.WindowMode.BORDERLESS
        position = Vector2f(200f)
    }
) {
    val camera = OrthographicCamera(window.currentSize).apply { rotate(0f, 180f, 0f) }

    var drawing = false

    init {
        window.addCursorInputs(object : CursorInputAdapter {
            override fun invokeClick(button: Key?, action: Keys.Action?, posX: Double, posY: Double): Boolean {
                drawing = button == Keys.LEFT_MOUSE.input
                        && action == Keys.Action.PRESS
                return false
            }
        })
    }

    val sdfBuffer = StorageBufferObject(100, SDFShader.SDFVertexAccessor)
    val pipeline = PostPassPipeline(SDFShader(), null, opaque = false)

    override fun loop(delta: Double) {}

    override fun render() {
        fun createCircle(position: Vector2fc, colour: Vector4fc) = SDFShader.SDFVertex(
                position = position,
                rotation = 0f,
                size = 0.5f,
                thickness = 0.025f,
                glowStrength = 50f,
                colour = colour,
                shapeType = SDFShader.SHAPE_CIRCLE,
                styleType = SDFShader.STYLE_LINE,
                layerType = SDFShader.LAYER_ADD,
                blendStrength = 1f
            )

        sdfBuffer.setData(
            listOf(
                createCircle((Vector2f(0f)).apply { x -= 100 }, Vector4f(1f, 0f, 0f, 0.8f)),
                createCircle((Vector2f(0f)), Vector4f(0f, 1f, 0f, 0.8f)),
                createCircle((Vector2f(0f)).apply { x += 100 }, Vector4f(0f, 0f, 1f, 0.8f))
            )
        )
        pipeline.shader.sdfLayers = sdfBuffer

        val primRuneEmpty = PrimaryRune("empty")
        renderCircle(
            Circle(
                null, listOf(primRuneEmpty, primRuneEmpty, primRuneEmpty, primRuneEmpty, primRuneEmpty),
                1, listOf()
            ), Vector2f(window.currentSize) / 2f, 0.5f, renderer
        )
    }
}
