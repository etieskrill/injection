package io.github.etieskrill.games.sails

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.dsl.std.rotationMat2
import io.github.etieskrill.injection.extension.shader.float
import io.github.etieskrill.injection.extension.shader.vec2
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.getComponent
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.Renderer
import org.etieskrill.engine.graphics.TextRenderer
import org.etieskrill.engine.graphics.camera.Camera
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.etieskrill.engine.graphics.gl.shader.impl.BlitShader
import org.etieskrill.engine.graphics.gl.shader.impl.ScreenSpacePointShader
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PipelineConfig
import org.etieskrill.engine.graphics.pipeline.PostPassPipeline
import org.etieskrill.engine.graphics.pipeline.PrimitiveType
import org.etieskrill.engine.graphics.text.Fonts
import org.etieskrill.engine.graphics.texture.Textures
import org.etieskrill.engine.window.Window
import org.joml.Math.PI_f
import org.joml.Math.toDegrees
import org.joml.Matrix2f
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector2i
import org.joml.Vector2ic
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4f
import org.joml.minus
import org.joml.plus
import org.joml.times
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.sqrt

class ShipRenderService(
    private val camera: Camera,
    private val renderer: Renderer,
    textRenderer: TextRenderer,
    private val window: Window
) : Service {

    private val shipSprite = Textures.ofFile("textures/ship-icon.png")
    private val pipeline = PostPassPipeline(BlitShader(), null, opaque = false, depthTest = false)

    private val hardpointPipeline = PostPassPipeline(HardpointShader(), null, opaque = false, depthTest = false)

    private val batch = Batch(renderer, textRenderer, window.currentSize).apply {
        combined = OrthographicCamera(window.currentSize).apply {
            setRotation(0f, 180f, 0f)
            setPosition(Vector3f(viewportSize, 0f).div(2f))
        }.combined
    }

    private val font = Fonts.getDefault(12)

    private val linePipeline = Pipeline(
        2,
        PipelineConfig(primitiveType = PrimitiveType.LINES),
        LineShader(),
        null
    )

    override fun canProcess(entity: Entity) = entity.hasComponents(
        NavalTransform::class.java,
        InputDirection::class.java,
        ShipStats::class.java
    )

    override fun process(
        targetEntity: Entity,
        entities: List<Entity>,
        delta: Double
    ) {
        val transform = targetEntity.getComponent<NavalTransform>()!!
        val inputDirection = targetEntity.getComponent<InputDirection>()!!
        val stats = targetEntity.getComponent<ShipStats>()!!

        when (stats.state) {
            ShipStats.State.ALIVE -> pipeline.shader.colour = Colour.WHITE
            ShipStats.State.DYING -> pipeline.shader.colour = Vector4f(1f, 1f, 1f, 1 - stats.deathProgress)
            ShipStats.State.DEAD -> return
        }

        pipeline.shader.apply {
            sprite = shipSprite
            useSpriteColour = true

            val spriteSize = Vector2f(transform.size)
            position = transform.position.toScreenSpace(camera) +
                    (spriteSize.div(
                        camera.aspectRatio,
                        Vector2f()
                    )) //FIXME this cannot be right. why does it even work?
            size = spriteSize * (camera.worldToView(Vector3f(1f, 0f, 0f)).x * camera.viewportSize.y())
            rotation = transform.rotation + PI_f

            windowSize = Vector2f(window.currentSize)
        }
        renderer.render(pipeline)

        for ((hardpoint, weapon) in stats.hardpoints) {
            hardpointPipeline.shader.apply {
                position = camera.worldToView(
                    Vector3f(
                        (transform.position + Matrix2f().rotation(transform.rotation) * hardpoint.position).div(
                            2f / camera.aspectRatio,
                            2f,
                            Vector2f()
                        ), 0f
                    )
                ).xy(Vector2f()).negateX()

                angle = toDegrees(hardpoint.angle + PI_f - transform.rotation)
                angleLimit = toDegrees(hardpoint.angleLimit)

                range = weapon?.range ?: 200f

                colour = Vector4f(1f, 0f, 0f, 0.2f)

                windowSize = window.currentSize.toFloat()
            }
            renderer.render(hardpointPipeline)
        }

        val healthBarPosition = transform.position.toScreenSpace(camera) + Vector2f(-25f, -6f * sqrt(transform.size))
        batch.renderBackground(healthBarPosition, Vector2f(50f, 5f), Colour.BLACK, 0f, Colour.BLACK)
        batch.renderBackground(
            healthBarPosition,
            Vector2f(50f * (stats.currentHealth.toFloat() / stats.maxHealth.toFloat()), 5f),
            if (!targetEntity.hasComponents(EnemyShipController::class.java)) Colour.GREEN else Colour.RED,
            0f, Colour.BLACK
        )
        batch.renderText(
            "${max(0, stats.currentHealth)}/${stats.maxHealth}",
            font, healthBarPosition + Vector2f(55f, -8f)
        )

        linePipeline.shader.apply {
            pointA = Vector3f(transform.position.toNDC(camera) - Vector2f(1f), 0f).negateY()
            pointB = Vector3f(
                (transform.position.toNDC(camera) - Vector2f(1f)
                        + (Matrix2f().rotation(transform.rotation + PI_f)
                        * Vector2f(inputDirection.direction).negateX() * 25f * log2(inputDirection.strength + 1))
                        / window.currentSize), 0f
            ).negateY()

            colour = Vector4f(1f, 0f, 1f, 1f)
        }
        renderer.render(linePipeline)
    }

}

class ProjectileRenderService(val camera: Camera, val renderer: Renderer, val window: Window) : Service {
    private val projectilePipeline = PostPassPipeline(ScreenSpacePointShader(), null, false, false)

    override fun canProcess(entity: Entity) = entity.hasComponents(Projectile::class.java)

    override fun process(targetEntity: Entity, entities: List<Entity>, delta: Double) {
        val projectile = targetEntity.getComponent<Projectile>()!!

        projectilePipeline.shader.apply {
            ndcPosition = camera.worldToView(Vector3f(projectile.position.div(2f, Vector2f()), 0f))
                .xy(Vector2f()).negateY()
            aspectRatio = window.aspectRatio
            size = projectile.size / window.currentSize.y()
            colour = Vector4f(0.15f, 0.15f, 0.15f, 1f)
        }
        renderer.render(projectilePipeline)
    }
}

fun Vector2fc.toNDC(camera: Camera) = camera.worldToView(Vector3f(this, 0f)).xy(Vector2f()) / 2f + Vector2f(1f)
fun Vector2fc.toScreenSpace(camera: Camera): Vector2f = toNDC(camera) * camera.viewportSize.toFloat() / 2f

object Colour {
    val BLACK = Vector4f(0f, 0f, 0f, 1f)
    val WHITE = Vector4f(1f)
    val RED = Vector4f(1f, 0f, 0f, 1f)
    val GREEN = Vector4f(0f, 1f, 0f, 1f)
}

fun Vector2fc.negateX() = Vector2f(-x(), y())
fun Vector2fc.negateY() = Vector2f(x(), -y())

fun Vector3fc.negateX() = Vector3f(-x(), y(), z())
fun Vector3fc.negateY() = Vector3f(x(), -y(), z())
fun Vector3fc.negateZ() = Vector3f(x(), y(), -z())

fun Vector2fc.toInt() = Vector2i(x().toInt(), y().toInt())
fun Vector2ic.toFloat() = Vector2f(x().toFloat(), y().toFloat())

operator fun Vector2fc.minus(other: Vector2ic) = Vector2f(x() - other.x(), y() - other.y())
operator fun Vector2fc.div(other: Vector2ic) = Vector2f(x() / other.x(), y() / other.y())

class HardpointShader : PureShaderBuilder<VertexData, ColourRenderTarget>( //TODO object for transpiler
    object : ShaderProgram(listOf("Hardpoint.glsl")) {}
) {
    val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))

    var position by uniform<vec2>()
    var angle by uniform<float>()
    var angleLimit by uniform<float>()
    var range by uniform<float>()

    var colour by uniform<vec4>()

    var windowSize by uniform<vec2>()

    override fun program() {
        vertex { VertexData(vec4(vertices[vertexID], 0, 1)) }
        fragment {
            val pos = it.position.xy
            pos.x *= (windowSize.x / windowSize.y)

            val rotAngle = angle * (Math.PI.toFloat() / 180f)
            pos.xy = rotationMat2(rotAngle) * (pos + position)

            val angle = clamp(angleLimit, 0.001, 179.999f)
            val triangle: Float
            val factor = tan((90f - angle) * (Math.PI / 180f))

            if (angle <= 90f) {
                if (pos.y > 0 && factor * abs(pos.x) / pos.y < 1) {
                    triangle = 1f
                } else {
                    triangle = 0f
                }
            } else {
                if (pos.y < 0 && factor * abs(pos.x) / pos.y < 1) {
                    triangle = 0f
                } else {
                    triangle = 1f
                }
            }

            val circle = if (length(pos) < range / windowSize.y) 1f else 0f

            val cone = if (triangle == 1f && circle == 1f) 1f else 0f

            ColourRenderTarget((vec4(cone) * colour).rt)
        }
    }
}

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
