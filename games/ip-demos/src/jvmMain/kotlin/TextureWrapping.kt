import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.ShaderVertexData
import io.github.etieskrill.injection.extension.shader.ivec2
import io.github.etieskrill.injection.extension.shader.sampler2D
import io.github.etieskrill.injection.extension.shader.vec2
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.scene.Batch
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.graphics.pipeline.PostPassPipeline
import org.etieskrill.engine.graphics.shader.Shader
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.graphics.texture.TextureType
import org.etieskrill.engine.graphics.texture.TextureWrapping
import org.etieskrill.engine.input.KeyInputHandler
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.KeyEventAction
import org.etieskrill.engine.input.MouseGestureHandler
import org.etieskrill.engine.scene.Node.Alignment
import org.etieskrill.engine.scene.Scene
import org.etieskrill.engine.scene.container.VBox
import org.etieskrill.engine.scene.element.Button
import org.etieskrill.engine.scene.element.Label
import org.etieskrill.engine.window.Cursor
import org.etieskrill.engine.window.CursorShape
import org.etieskrill.engine.window.Window
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.plus
import org.joml.unaryMinus
import org.lwjgl.opengl.GL11C.GL_TRIANGLE_STRIP
import org.lwjgl.opengl.GL11C.glDrawArrays
import org.lwjgl.opengl.GL30C.glBindVertexArray
import org.lwjgl.opengl.GL30C.glGenVertexArrays

fun main() {
    App().run()
}

class App : org.etieskrill.engine.application.App(
    Window(
        resizeable = true,
        cursor = Cursor(CursorShape.RESIZE_ALL),
        refreshRate = 10000u,
        transparency = true
    )
) {
    var wrapping: Wrapping = Wrapping.NONE
        set(value) {
            field = value
            texture.wrapping = when (value) {
                Wrapping.NONE -> TextureWrapping.CLAMP_TO_BORDER
                Wrapping.CLAMP_TO_EDGE -> TextureWrapping.CLAMP_TO_EDGE
                Wrapping.REPEAT -> TextureWrapping.REPEAT
                Wrapping.MIRROR -> TextureWrapping.MIRRORED_REPEAT
            }
        }

    val texture: Texture2D = Texture2D.createFromFile("textures/lena_rgb.png", TextureType.DIFFUSE)
    val shader: TextureWrappingShader = TextureWrappingShader()
    val dummyVAO: Int = glGenVertexArrays()

    val textureOffset: Vector2d = Vector2d(-Vector2i(window.size) / 2 + Vector2i(100))
    val textureSize = Vector2f(200f)

    val fpsLabel = Label()
    val modeLabel = Label()

    init {
        wrapping = Wrapping.NONE

        window.keyInputs += KeyInputHandler { event ->
            if (event.key == Key.E && event.action == KeyEventAction.PRESS) {
                nextMode()
                true
            } else if (event.key == Key.Q && event.action == KeyEventAction.PRESS) {
                previousMode()
                true
            } else {
                false
            }
        }

        window.cursorInputs += object : MouseGestureHandler() {
            override fun invokeDrag(deltaX: Double, deltaY: Double, posX: Double, posY: Double): Boolean {
                textureOffset.x += deltaX
                textureOffset.y += deltaY
                return true
            }

            override fun invokeScroll(deltaX: Double, deltaY: Double): Boolean {
                textureSize.add((deltaY.toFloat() / 20) * textureSize.x, (deltaY.toFloat() / 20) * textureSize.y)
                return true
            }
        }

        window.scene = Scene(
            Batch(window.screenBuffer, renderer, textRenderer),
            VBox(
                fpsLabel,
                modeLabel,
                Label("Press '${Key.Q}' and '${Key.E}' to cycle modes\nMouse drag to move\nScroll to zoom"),
                Button(Label("Previous mode").apply { alignment = Alignment.CENTER }
                ) { previousMode() }.apply {
                    alignment = Alignment.BOTTOM_LEFT
                    size = Vector2f(150f, 50f)
                },
                Button(Label("Next mode").apply { alignment = Alignment.CENTER }
                ) { nextMode() }.apply {
                    alignment = Alignment.BOTTOM_RIGHT
                    size = Vector2f(150f, 50f)
                },
//                HBox( //FIXME conclusively, the HBox is fucked
//                    Button(Label("Previous mode")).apply {
//                        setAction {
//                            wrapping = TextureWrapping.entries[(wrapping.ordinal - 1) % TextureWrapping.entries.size]
//                        }
//                    }.apply { alignment = Alignment.BOTTOM_LEFT },
//                    Button(Label("Next mode")).apply {
//                        setAction {
//                            wrapping = TextureWrapping.entries[(wrapping.ordinal + 1) % TextureWrapping.entries.size]
//                        }
//                    }.apply { alignment = Alignment.BOTTOM_RIGHT }
//                ).apply { alignment = Node.Alignment.BOTTOM }
            ),
            OrthographicCamera(window.size),
        )
    }

    private fun nextMode() {
        wrapping = Wrapping.entries[(wrapping.ordinal + 1) % TextureWrapping.entries.size]
    }

    private fun previousMode() {
        wrapping = Wrapping.entries[(wrapping.ordinal - 1)
            .let { if (it < 0) it + Wrapping.entries.size else it } % Wrapping.entries.size]
    }

    override fun loop(delta: Double) {
        shader.targetTexture = texture
        shader.windowSize = window.size
        shader.offset = Vector2f(textureOffset)
        shader.targetTextureSize = textureSize

        val pipeline = PostPassPipeline(shader, window.screenBuffer)
        renderer.render(pipeline)

        fpsLabel.text = "FPS: %.0f".format(pacer.averageFPS)
        modeLabel.text = "Current mode: $wrapping"
    }
}

enum class Wrapping { NONE, CLAMP_TO_EDGE, REPEAT, MIRROR } //none is CLAMP_TO_BORDER - so long as border is black

class TextureWrappingShader : PureShaderBuilder<TextureWrappingShader.Vertex, ColourRenderTarget>(
    object : Shader(listOf("TextureWrapping.glsl")) {}
) {
    data class Vertex(override val position: vec4, val texCoords: vec2) : ShaderVertexData

    val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    var targetTexture by uniform<sampler2D>() //TODO actually buffer these and only set when rendering
    var targetTextureSize by uniform<vec2>(/*vec2(200)*/)
    var windowSize by uniform<ivec2>()

    var offset by uniform<vec2>()

    override fun program() {
        vertex {
            val texCoords = max(vertices[vertexID], 0)
            texCoords.y = 1 - texCoords.y

            Vertex(
                position = vec4(vertices[vertexID], 0, 1),
                texCoords = texCoords
            )
        }
        fragment {
            var scaledCoords = it.texCoords * (windowSize / targetTextureSize)
            scaledCoords =
                scaledCoords.plus(offset / targetTextureSize) //the +=/+ conflict is just joml's operators being funky - and no, i'm not gonna fix plusAssign

            val texel = texture(targetTexture, scaledCoords).rgb
            val alpha = if (texel == vec3(0)) 0.75 else 1.0

            ColourRenderTarget(vec4(texel, alpha))
        }
    }
}
