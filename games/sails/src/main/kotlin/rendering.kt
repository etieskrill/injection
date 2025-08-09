package io.github.etieskrill.games.sails

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.getComponent
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.engine.graphics.Renderer
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.etieskrill.engine.graphics.gl.shader.impl.BlitShader
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PipelineConfig
import org.etieskrill.engine.graphics.pipeline.PostPassPipeline
import org.etieskrill.engine.graphics.pipeline.PrimitiveType
import org.etieskrill.engine.graphics.texture.Textures
import org.etieskrill.engine.window.Window
import org.joml.Matrix2f
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector2i
import org.joml.Vector2ic
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.minus
import org.joml.plus
import org.joml.times

class ShipRenderService(private val renderer: Renderer, private val window: Window) : Service {

    private val shipSprite = Textures.ofFile("textures/ship-icon.png")
    private val pipeline = PostPassPipeline(BlitShader(), null, opaque = false, depthTest = false)

    private val linePipeline = Pipeline(
        2,
        PipelineConfig(primitiveType = PrimitiveType.LINES),
        LineShader(),
        null
    )

    override fun canProcess(entity: Entity) = entity.hasComponents(
        NavalTransform::class.java,
        InputDirection::class.java
    )

    override fun process(
        targetEntity: Entity,
        entities: List<Entity?>,
        delta: Double
    ) {
        val transform = targetEntity.getComponent<NavalTransform>()!!
        val inputDirection = targetEntity.getComponent<InputDirection>()!!

        pipeline.shader.apply {
            sprite = shipSprite
            useSpriteColour = true

            val spriteSize = Vector2f(100f)
            position = transform.position - spriteSize.div(2f, Vector2f())
            rotation = transform.rotation + org.joml.Math.PI.toFloat()

            windowSize = Vector2f(window.currentSize)
        }

        renderer.render(pipeline)

        linePipeline.shader.apply {
            pointA = Vector3f((transform.position * 2f - window.currentSize) / window.currentSize, 0f)
                .apply { y *= -1 }
            pointB = Vector3f(
                (transform.position * 2f - window.currentSize
                        + Matrix2f().rotation(transform.rotation)
                        * Vector2f(inputDirection.direction).apply { x *= -1 } * inputDirection.strength * 100f)
                        / window.currentSize, 0f
            ).apply { y *= -1 }

            colour = Vector4f(1f, 0f, 1f, 1f)
        }
        renderer.render(linePipeline)
    }

}

fun Vector2fc.toInt() = Vector2i(x().toInt(), y().toInt())
fun Vector2ic.toFloat() = Vector2f(x().toFloat(), y().toFloat())

operator fun Vector2fc.minus(other: Vector2ic) = Vector2f(x() - other.x(), y() - other.y())
operator fun Vector2fc.div(other: Vector2ic) = Vector2f(x() / other.x(), y() / other.y())

class LineShader : PureShaderBuilder<VertexData, ColourRenderTarget>(
    object : ShaderProgram(listOf("Line.glsl")) {}
) {
    var pointA by uniform<vec3>()
    var pointB by uniform<vec3>()

    var colour by uniform<vec4>()

    override fun program() {
//        vertex { VertexData(vec4(if (vertexID == 0) pointA else pointB, 1)) } //FIXME ruh oh
        vertex {
            val point = if (vertexID == 0) pointA else pointB
            VertexData(vec4(point, 1))
        }
        fragment { ColourRenderTarget(colour.rt) }
    }
}
