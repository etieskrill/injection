package io.github.etieskrill.games.circles

import org.etieskrill.engine.application.GameApplication
import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment
import org.etieskrill.engine.graphics.pipeline.PostPassPipeline
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.input.CursorInputAdapter
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.scene.Scene
import org.etieskrill.engine.scene.component.Button
import org.etieskrill.engine.scene.component.Label
import org.etieskrill.engine.scene.component.VBox
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.window
import org.joml.Vector2f
import org.joml.Vector4f

fun main() {
    Main().run()
}

class Main : GameApplication(
    window {
        title = "Circles"
        size = Window.WindowSize.XGA
        mode = Window.WindowMode.BORDERLESS
        position = Vector2f(200f)
    }
) {
    val camera = OrthographicCamera(window.currentSize)

    val paintTexture = Texture2D.BlankBuilder(window.currentSize).build()
    val paintFrameBuffer = FrameBuffer.Builder(window.currentSize)
        .attach(paintTexture, FrameBufferAttachment.BufferAttachmentType.COLOUR0)
        .build()
    val paintPipeline = PostPassPipeline(
        PaintShader(),
        paintFrameBuffer,
        opaque = false,
        depthTest = false
    )

    val paintFeedbackPipeline = PostPassPipeline(
        FullScreenTextureShader(),
        null,
        opaque = false,
        depthTest = false
    )

    var drawing = false

    init {
        window.scene = Scene(
            Batch(renderer, window.currentSize),
            VBox(
                Button(Label("Circle")).apply {
                    size = Vector2f(200f, 50f)
                    margin = Vector4f(10f)
                    setAction { pipeline.shader.shapeType = CircleShader.CIRCLE }
                },
                Button(Label("Fire rune")).apply {
                    size = Vector2f(200f, 50f)
                    margin = Vector4f(10f)
                    setAction { pipeline.shader.shapeType = CircleShader.FIRE_RUNE }
                }
            ),
            camera
        )

        window.addCursorInputs(object : CursorInputAdapter {
            override fun invokeClick(button: Key?, action: Keys.Action?, posX: Double, posY: Double): Boolean {
                drawing = button == Keys.LEFT_MOUSE.input
                        && action == Keys.Action.PRESS
                return false
            }
        })
    }

    val pipeline = PostPassPipeline(CircleShader(), null)

    override fun loop(delta: Double) {}

    override fun render() {
        pipeline.shader.apply {
            cursorPosition = Vector2f(window.cursor.position)
            size = .2f
            combined = camera.combined
            aspect = camera.viewportSize.x().toFloat() / camera.viewportSize.y()
        }

        renderer.render(pipeline)

        if (drawing) {
            paintPipeline.shader.apply {
                position = Vector2f(window.cursor.position).div(Vector2f(window.currentSize))
                size = Vector2f(1f).div(Vector2f(window.currentSize))
            }
            renderer.render(paintPipeline)
        }
        FrameBuffer.bindScreenBuffer()

        //FIXME probably does not render because of culling
        paintFeedbackPipeline.shader.sprite = paintTexture
        renderer.render(paintFeedbackPipeline)
    }
}
