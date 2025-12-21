package io.github.etieskrill.games.sails

import io.github.etieskrill.injection.extension.shader.dsl.ColourRenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.dsl.std.rotationMat2
import io.github.etieskrill.injection.extension.shader.float
import io.github.etieskrill.injection.extension.shader.vec2
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
import org.etieskrill.engine.graphics.gl.shader.impl.LineShader
import org.etieskrill.engine.graphics.gl.shader.impl.ScreenSpacePointShader
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PipelineConfig
import org.etieskrill.engine.graphics.pipeline.PostPassPipeline
import org.etieskrill.engine.graphics.pipeline.PrimitiveType
import org.etieskrill.engine.graphics.text.Fonts
import org.etieskrill.engine.graphics.texture.Textures
import org.etieskrill.engine.window.Window
import org.joml.Math.PI_OVER_2_f
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
    window: Window
) : Service {

    private val shipSprite = Textures.ofFile("textures/ship-icon.png")

    private val hardpointPipeline = PostPassPipeline(HardpointShader(), null, opaque = false, depthTest = false)

    private val batch = Batch(renderer, textRenderer, window.currentSize).apply {
        combined = OrthographicCamera(viewportSize).apply {
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

        val spriteColour = when (stats.state) {
            ShipStats.State.ALIVE -> Colour.WHITE
            ShipStats.State.DYING -> Vector4f(1f, 1f, 1f, 1 - stats.deathProgress)
            ShipStats.State.DEAD -> return
        }

        val spriteSize = Vector2f(transform.size) * (batch.viewportSize.toFloat() / camera.viewportSize)
        batch.blit(
            shipSprite,
            batch.ndcToScreenSpace((transform.position).toNDC(camera)) - Vector2f(spriteSize) / 2f,
            spriteSize,
            transform.rotation,
            spriteColour
        )

        for ((hardpoint, weapon) in stats.hardpoints) {
            hardpointPipeline.shader.apply {
                position = (transform.position + Matrix2f().rotation(transform.rotation) * hardpoint.position)
                    .toNDC(camera).negateX()

                angle = -hardpoint.angle - transform.rotation
                angleLimit = hardpoint.angleLimit

                range = (weapon?.range ?: 200f) / (camera.viewportSize.y() / camera.aspectRatio) * 1.095f //¯\_(ツ)_/¯

                colour = Vector4f(1f, 0f, 0f, 0.2f)

                aspectRatio = camera.aspectRatio
            }
            renderer.render(hardpointPipeline)
        }

        val healthBarPosition = batch.ndcToScreenSpace(transform.position.toNDC(camera)) +
                Vector2f(
                    -25f,
                    -6f * sqrt(transform.size) * (batch.viewportSize.y().toFloat() / camera.viewportSize.y())
                )
        batch.renderBackground(healthBarPosition, Vector2f(50f, 5f), Colour.BLACK, 0f, Colour.BLACK)
        batch.renderBackground(
            healthBarPosition,
            Vector2f(50f * (stats.currentHealth.toFloat() / stats.maxHealth.toFloat()), 5f),
            if (stats.faction == PLAYER_FACTION) Colour.GREEN else Colour.RED,
            0f, Colour.BLACK
        )
        batch.renderText(
            "${max(0, stats.currentHealth)}/${stats.maxHealth}",
            font, healthBarPosition + Vector2f(55f, -8f)
        )

        linePipeline.shader.apply {
            pointA = Vector3f(transform.position.toNDC(camera), 0f).negateY()
            val offset = (Matrix2f().rotation(transform.rotation)
                    * Vector2f(inputDirection.direction).negateX() * 15f * log2(inputDirection.strength + 1))
            pointB = Vector3f((transform.position + offset).toNDC(camera), 0f).negateY()

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
            ndcPosition = projectile.position.toNDC(camera).negateY()
            aspectRatio = window.aspectRatio
            size = projectile.size / window.currentSize.y()
            colour = Vector4f(0.15f, 0.15f, 0.15f, 1f)
        }
        renderer.render(projectilePipeline)
    }
}

fun Vector2fc.toNDC(camera: Camera) = camera.worldToView(Vector3f(this, 0f)).xy(Vector2f())!!

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

    var aspectRatio by uniform<float>()

    override fun program() {
        vertex { VertexData(vec4(vertices[vertexID], 0, 1)) }
        fragment {
            val normalPos = vec2(it.position.x * aspectRatio, it.position.y) +
                    vec2(position.x * aspectRatio, position.y)
            val pos = rotationMat2(angle) * normalPos

            val angle = clamp(angleLimit, 0.0001, Math.PI - 0.0001)
            val triangle: Float
            val factor = tan(PI_OVER_2_f - angle)

            if (angle <= PI_OVER_2_f) {
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

            val circle = if (length(pos) < range) 1f else 0f

            val cone = if (triangle == 1f && circle == 1f) 1f else 0f

            ColourRenderTarget((vec4(cone) * colour).rt)
        }
    }
}
