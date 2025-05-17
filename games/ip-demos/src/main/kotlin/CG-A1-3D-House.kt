import io.github.etieskrill.injection.extension.shader.dsl.RenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.ShaderVertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.application.GameApplication
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.graphics.gl.BufferObject
import org.etieskrill.engine.graphics.gl.VertexArrayAccessor
import org.etieskrill.engine.graphics.gl.VertexArrayObject
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.etieskrill.engine.input.controller.CursorCameraController
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.times
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11C.*

val houseVertices = floatArrayOf(
    -1f, -1f, 1f, 1f, 1f, 0f, //0
    1f, -1f, 1f, 1f, 1f, 0f, //1
    -1f, 1f, 1f, 1f, 1f, 0f,  //2
    1f, 1f, 1f, 1f, 1f, 0f,  //3
    -1f, -1f, -1f, 1f, 1f, 0f,  //4
    1f, -1f, -1f, 1f, 1f, 0f,  //5
    -1f, 1f, -1f, 1f, 1f, 0f,  //6
    1f, 1f, -1f, 1f, 1f, 0f,  //7

    0f, 1.5f, 0f, 0f, 0f, 1f
)
val houseIndices = intArrayOf(
    //Top
    2, 6, 7,
    2, 3, 7,

    //Bottom
    5, 4, 0,
    0, 1, 5,

    //Left
    6, 2, 0,
    0, 4, 6,

    //Right
    1, 3, 7,
    1, 5, 7,

    //Front
    2, 2, 0,
    0, 1, 3,

    //Back
    7, 6, 4,
    4, 5, 7,

    8, 6, 2,
)

data class Vertex(val position: Vector3f, val colour: Vector3f)
object VertexAccessor : VertexArrayAccessor<Vertex>() {
    override fun registerFields() {
        addField(Vector3f::class.java) { vertex, buffer -> vertex.position.get(buffer) }
        addField(Vector3f::class.java) { vertex, buffer -> vertex.colour.get(buffer) }
    }
}

fun main() {
    `CG-03-A`().run()
}

class `CG-03-A` : GameApplication() {
    val houseVAO = VertexArrayObject.builder(VertexAccessor)
        .vertexBuffer(BufferObject.create(BufferUtils.createFloatBuffer(houseVertices.size).put(houseVertices)).build())
        .indices(houseIndices.toMutableList())
        .build()

    val shader = BasicShader()

    val camera = OrthographicCamera(window.currentSize)
        .apply {
            setOrbit(true)
            setOrbitDistance(200f)
            setRotation(45f, 45f, 0f)

            setFar(500f)
        }

    init {
        window.addCursorInputs(CursorCameraController(camera))
        window.cursor.disable()
    }

    override fun loop(delta: Double) {
        houseVAO.bind()
        shader.transform = Matrix4f(camera.combined) * Matrix4f().scale(100f, -100f, 100f)
//        shader.transform = Matrix4f()
//            .lookAt(Vector3f(1f), Vector3f(0f), Vector3f(0f, 1f, 0f))
//            .ortho(
//                -.5f * window.currentSize.x(), .5f * window.currentSize.x(),
//                .5f * window.currentSize.y(), -.5f * window.currentSize.y(),
//                0.1f, 100f
//            )
        shader.start()

//        glDisable(GL_CULL_FACE)
        glDrawElements(GL_TRIANGLES, houseIndices.size, GL_UNSIGNED_INT, 0)
    }
}

class BasicShader : ShaderBuilder<Vertex, BasicShader.VertexData, BasicShader.RenderTargets>(
    object : ShaderProgram(listOf("Basic.glsl")) {}
) {
    data class VertexData(override val position: vec4, val colour: vec3) : ShaderVertexData
    data class RenderTargets(val colour: RenderTarget)

    var transform by uniform<mat4>()

    override fun program() {
        vertex {
            VertexData(transform * vec4(it.position, 1), it.colour)
        }
        fragment {
            RenderTargets(vec4(it.colour, 1).rt)
        }
    }
}
