import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.application.App
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.graphics.gl.VertexArrayAccessor
import org.etieskrill.engine.graphics.gl.VertexArrayObject
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.etieskrill.engine.graphics.pipeline.CullingMode
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PipelineConfig
import org.etieskrill.engine.graphics.pipeline.PrimitiveType.*
import org.joml.Math
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.plus

val alphabet = listOf(
    'X', //noop
    'F', //draw and move forward
    '+', //turn left
    '-', //turn right
    '[', //push position and rotation to stack
    ']' //pop position and rotation from stack
)

val rules = mapOf(
    "X" to "F+[[X]-X]-F[-FX]+X",
    "F" to "FF"
)

val start = "-X"

val step = 1f
val angle = Math.toRadians(25.0).toFloat()

fun iterateLSystem(start: String, rules: Map<String, String>, numIterations: Int): String {
    if (numIterations <= 0) return start

    var output = start
    var nextOutput = ""

    repeat(numIterations) {
        for (c in output) {
            if (c.toString() in rules) {
                nextOutput += rules[c.toString()]!!
            } else {
                nextOutput += c
            }
        }

        output = nextOutput
        nextOutput = ""
    }

    return output
}

class Vertex(val position: Vector2fc)
object VertexAccessor : VertexArrayAccessor<Vertex>() {
    override fun registerFields() {
        addField(Vector2fc::class.java) { vertex, buffer -> vertex.position.get(buffer) }
    }
}

class DirectLineShader : ShaderBuilder<Vertex, VertexData, ColourRenderTarget>(
    object : ShaderProgram(listOf("DirectLine.glsl"), false) {}
) {
    //TODO primitive mode should be bound to shader & maybe fallback of element count if no vao present
    var colour by uniform<vec4>()
    var combined by uniform<mat4>()

    override fun program() {
        vertex { VertexData(combined * vec4(it.position, 0, 1)) }
        fragment { ColourRenderTarget(colour.rt) }
    }
}

class LSystemDemo(lines: List<Pair<Vector2fc, Vector2fc>>) : App() {
    init {
        window.currentSize = Vector2i(1000)
    }

    val buffer = FrameBuffer.getColour(Vector2i(600))
    val vao = VertexArrayObject.builder(VertexAccessor)
        .vertexElements(lines.flatMap { listOf(Vertex(it.first), Vertex(it.second)) })
        .build()
    val pipeline = Pipeline(
        vao,
        PipelineConfig(
            primitiveType = LINES,
            cullingMode = CullingMode.NONE,
            lineWidth = 2f,
        ),
        DirectLineShader().apply {
            colour = Vector4f(1f, 0f, 0f, 1f)
            combined = OrthographicCamera(window.currentSize).combined
        },
        null //buffer
    )

    val points = lines.flatMap { listOf(it.first, it.second) }
    val minPoint = Vector2f(points.minBy { it.x() }.x(), points.minBy { it.y() }.y())
    val maxPoint = Vector2f(points.maxBy { it.x() }.x(), points.maxBy { it.y() }.y())

    override fun loop(delta: Double) {
        pipeline.shader.combined = OrthographicCamera(window.currentSize).apply {
            val centre = (maxPoint + minPoint) / 2f
            position = Vector3f(centre.x, centre.y, 0f)
        }.combined
    }

    override fun render() {
        renderer.render(pipeline)
    }
}

fun main() {
    val fernMaybe = iterateLSystem(start, rules, 6)
        .replace("X", "")

    val stack = mutableListOf<Pair<Vector2fc, Float>>()

    val position = Vector2f()
    var rotation = Math.toRadians(-90f)
    val lines = mutableListOf<Pair<Vector2fc, Vector2fc>>()
    for (step in fernMaybe) {
        when (step) {
            'F' -> lines.add(Vector2f(position) to Vector2f(position.apply {
                add(
                    3f * Math.cos(rotation),
                    3f * Math.sin(rotation)
                )
            }))

            '+' -> rotation -= Math.toRadians(20f)
            '-' -> rotation += Math.toRadians(20f)
            '[' -> stack.add(Vector2f(position) to rotation)
            ']' -> {
                val (pos, rot) = stack.removeLast()
                position.set(pos); rotation = rot
            }
        }
    }

    LSystemDemo(lines).run()
}
