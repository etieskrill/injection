import io.github.etieskrill.injection.extension.shader.*
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.RenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderVertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import org.etieskrill.engine.application.GameApplication
import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.etieskrill.engine.graphics.texture.AbstractTexture
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.input.CursorInputAdapter
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.scene.Scene
import org.etieskrill.engine.scene.component.Button
import org.etieskrill.engine.scene.component.Label
import org.etieskrill.engine.scene.component.Node.Alignment
import org.etieskrill.engine.scene.component.VBox
import org.etieskrill.engine.window.Cursor
import org.etieskrill.engine.window.window
import org.joml.*
import org.lwjgl.opengl.GL11C.*
import org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL12C.GL_TEXTURE_WRAP_R
import org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER
import org.lwjgl.opengl.GL30C.glBindVertexArray
import org.lwjgl.opengl.GL30C.glGenVertexArrays

fun main() {
    App()
}

class App : GameApplication(window {
    resizeable = true
    cursor = Cursor.getDefault(Cursor.CursorShape.RESIZE_ALL)
    refreshRate = 10000
}) {
    var wrapping: TextureWrapping = TextureWrapping.NONE
        set(value) {
            field = value
            setTextureWrapping(value)
        }

    lateinit var texture: Texture2D
    lateinit var shader: TextureWrappingShader
    var dummyVAO: Int = -1

    lateinit var textureOffset: Vector2d

    lateinit var fpsLabel: Label
    lateinit var modeLabel: Label

    override fun init() {
        texture = Texture2D.FileBuilder("lena_rgb.png").build()

        shader = TextureWrappingShader()
        dummyVAO = glGenVertexArrays()

        wrapping = TextureWrapping.NONE
        window.addKeyInputs { type, key, action, modifiers ->
            if (key == Keys.E.input.value && action == 1) {
                wrapping = TextureWrapping.entries[(wrapping.ordinal + 1) % TextureWrapping.entries.size]
            }

            false
        }

        textureOffset = Vector2d(-Vector2i(window.currentSize) / 2 + Vector2i(100))

        window.addCursorInputs(object : CursorInputAdapter {
            var clicked = false
            override fun invokeClick(button: Key?, action: Int, posX: Double, posY: Double): Boolean {
                if (button == Keys.LEFT_MOUSE.input) {
                    clicked = action == 1
                    prevX = posX
                    prevY = posY
                }

                return false
            }

            var prevX: Double? = null
            var prevY: Double? = null
            override fun invokeMove(posX: Double, posY: Double): Boolean {
                if (!clicked) return false

                if (prevX == null || prevY == null) {
                    prevX = posX
                    prevY = posY
                    return false
                }

                textureOffset.x += prevX!! - posX
                textureOffset.y += prevY!! - posY

                prevX = posX
                prevY = posY

                return false
            }
        })

        fpsLabel = Label()
        modeLabel = Label()
        window.scene = Scene(
            Batch(renderer),
            VBox(
                fpsLabel, modeLabel, Label("Press '${Keys.E}' to cycle mode"), Label("Mouse drag to move"),
                Button(Label("Previous mode").apply { alignment = Alignment.CENTER }).apply {
                    setAction {
                        wrapping = TextureWrapping.entries[(wrapping.ordinal - 1)
                            .let { if (it < 0) it + TextureWrapping.entries.size else it } % TextureWrapping.entries.size]
                    }
                }.apply { alignment = Alignment.BOTTOM_LEFT; size = Vector2f(150f, 50f) },
                Button(Label("Next mode").apply { alignment = Alignment.CENTER }).apply {
                    setAction {
                        wrapping = TextureWrapping.entries[(wrapping.ordinal + 1) % TextureWrapping.entries.size]
                    }
                }.apply { alignment = Alignment.BOTTOM_RIGHT; size = Vector2f(150f, 50f) },
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

    private fun setTextureWrapping(wrapping: TextureWrapping) {
        val glWrapping = when (wrapping) {
            TextureWrapping.NONE -> GL_CLAMP_TO_BORDER
            TextureWrapping.CLAMP_TO_EDGE -> GL_CLAMP_TO_EDGE
            TextureWrapping.REPEAT -> GL_REPEAT
            TextureWrapping.MIRROR -> {
                setTextureWrapping(TextureWrapping.REPEAT)
                shader.mirrorTexCords = true
                return
            }
        }

        shader.mirrorTexCords = false

        texture.bind()
        glTexParameteri(AbstractTexture.Target.TWO_D.gl(), GL_TEXTURE_WRAP_S, glWrapping)
        glTexParameteri(AbstractTexture.Target.TWO_D.gl(), GL_TEXTURE_WRAP_T, glWrapping)
        glTexParameteri(AbstractTexture.Target.TWO_D.gl(), GL_TEXTURE_WRAP_R, glWrapping)
    }

    override fun loop(delta: Double) {
        check(dummyVAO != -1)
        glBindVertexArray(dummyVAO)
        shader.start()
        shader.targetTexture = texture
        shader.windowSize = window.currentSize
        shader.offset = Vector2f(textureOffset)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)

        fpsLabel.text = "FPS: %.0f".format(pacer.averageFPS)
        modeLabel.text = "Current mode: $wrapping"
    }
}

enum class TextureWrapping { NONE, CLAMP_TO_EDGE, REPEAT, MIRROR } //none is CLAMP_TO_BORDER - so long as border is black

abstract class WrapperShader<T1 : ShaderVertexData, T2 : Any>(shader: ShaderProgram) : PureShaderBuilder<T1, T2>(shader)

class TextureWrappingShader : WrapperShader<TextureWrappingShader.Vertex, TextureWrappingShader.RenderTargets>(
    object : ShaderProgram(listOf("TextureWrapping.glsl")) {}
) {
    data class Vertex(override val position: vec4, val texCoords: vec2) : ShaderVertexData
    data class RenderTargets(val colour: RenderTarget)

    val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))
    val targetTextureSize by const(vec2(200))

    var targetTexture by uniform<sampler2D>()
    var windowSize by uniform<ivec2>()
    var mirrorTexCords by uniform<bool>()

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

            if (mirrorTexCords) {
                val reduced =
                    scaledCoords % 2 //there's this weird border when mirrored and at odd pixel positions - probs a floating point error somewhere here
                if (reduced.x > 1) scaledCoords.x = 1 - scaledCoords.x
                if (reduced.y > 1) scaledCoords.y = 1 - scaledCoords.y
                //if (reduced.y > 1) scaledCoords.y += 1 - scaledCoords.y //TODO put on test spanking bench
            }

            RenderTargets(vec4(texture(targetTexture, scaledCoords).rgb, 1).rt)
        }
    }
}
