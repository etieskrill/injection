import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.RenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderVertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.ivec2
import io.github.etieskrill.injection.extension.shader.sampler2D
import io.github.etieskrill.injection.extension.shader.vec2
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.application.GameApplication
import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.etieskrill.engine.graphics.texture.AbstractTexture.Wrapping
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.input.MouseGestureHandler
import org.etieskrill.engine.scene.Scene
import org.etieskrill.engine.scene.component.Button
import org.etieskrill.engine.scene.component.Label
import org.etieskrill.engine.scene.component.Node.Alignment
import org.etieskrill.engine.scene.component.VBox
import org.etieskrill.engine.window.Cursor
import org.etieskrill.engine.window.window
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

class App : GameApplication(window {
    resizeable = true
    cursor = Cursor.getDefault(Cursor.CursorShape.RESIZE_ALL)
    refreshRate = 10000
    transparency = true
}) {
    var wrapping: TextureWrapping = TextureWrapping.NONE
        set(value) {
            field = value
            texture.setWrapping(
                when (value) {
                    TextureWrapping.NONE -> Wrapping.CLAMP_TO_BORDER
                    TextureWrapping.CLAMP_TO_EDGE -> Wrapping.CLAMP_TO_EDGE
                    TextureWrapping.REPEAT -> Wrapping.REPEAT
                    TextureWrapping.MIRROR -> Wrapping.MIRRORED_REPEAT
                }
            )
        }

    val texture: Texture2D = Texture2D.FileBuilder("lena_rgb.png").build()
    val shader: TextureWrappingShader = TextureWrappingShader()
    val dummyVAO: Int = glGenVertexArrays()

    val textureOffset: Vector2d = Vector2d(-Vector2i(window.currentSize) / 2 + Vector2i(100))
    val textureSize = Vector2f(200f)

    val fpsLabel = Label()
    val modeLabel = Label()

    init {
        wrapping = TextureWrapping.NONE

        window.addKeyInputs { type, key, action, modifiers ->
            if (key == Keys.E.input.value && action == 1) nextMode()
            else if (key == Keys.Q.input.value && action == 1) previousMode()
            else return@addKeyInputs true
            false
        }

        window.addCursorInputs(object : MouseGestureHandler() {
            override fun invokeDrag(deltaX: Double, deltaY: Double, posX: Double, posY: Double): Boolean {
                textureOffset.x += deltaX
                textureOffset.y += deltaY
                return true
            }
            override fun invokeScroll(deltaX: Double, deltaY: Double): Boolean {
                textureSize.add((deltaY.toFloat() / 20) * textureSize.x, (deltaY.toFloat() / 20) * textureSize.y)
                return true
            }
        })

        window.scene = Scene(
            Batch(renderer, window.currentSize),
            VBox(
                fpsLabel,
                modeLabel,
                Label("Press '${Keys.Q}' and '${Keys.E}' to cycle modes\nMouse drag to move\nScroll to zoom"),
                Button(Label("Previous mode").apply { alignment = Alignment.CENTER }).apply {
                    setAction { previousMode() }
                    alignment = Alignment.BOTTOM_LEFT
                    size = Vector2f(150f, 50f)
                },
                Button(Label("Next mode").apply { alignment = Alignment.CENTER }).apply {
                    setAction { nextMode() }
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
            OrthographicCamera(window.size.vec),
        )
    }

    private fun nextMode() {
        wrapping = TextureWrapping.entries[(wrapping.ordinal + 1) % TextureWrapping.entries.size]
    }

    private fun previousMode() {
        wrapping = TextureWrapping.entries[(wrapping.ordinal - 1)
            .let { if (it < 0) it + TextureWrapping.entries.size else it } % TextureWrapping.entries.size]
    }

    override fun loop(delta: Double) {
        check(dummyVAO != -1)
        glBindVertexArray(dummyVAO)
        shader.start()
        shader.targetTexture = texture
        shader.windowSize = window.currentSize
        shader.offset = Vector2f(textureOffset)
        shader.targetTextureSize = textureSize
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)

        fpsLabel.text = "FPS: %.0f".format(pacer.averageFPS)
        modeLabel.text = "Current mode: $wrapping"
    }
}

enum class TextureWrapping { NONE, CLAMP_TO_EDGE, REPEAT, MIRROR } //none is CLAMP_TO_BORDER - so long as border is black

class TextureWrappingShader : PureShaderBuilder<TextureWrappingShader.Vertex, TextureWrappingShader.RenderTargets>(
    object : ShaderProgram(listOf("TextureWrapping.glsl")) {}
) {
    data class Vertex(override val position: vec4, val texCoords: vec2) : ShaderVertexData
    data class RenderTargets(val colour: RenderTarget)

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

            RenderTargets(vec4(texel, alpha).rt)
        }
    }
}
