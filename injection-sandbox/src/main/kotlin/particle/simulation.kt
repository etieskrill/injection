package particle

import org.etieskrill.engine.application.App
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.impl.RenderService
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.particle.ParticleEmitter
import org.etieskrill.engine.graphics.particle.ParticleNode
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.joml.Vector4f
import org.joml.minus
import org.joml.plus
import org.joml.primitives.AABBf
import org.joml.primitives.AABBfc
import org.joml.times

class FireParticle(
    val position: Vector3f = Vector3f(),
    val prevPosition: Vector3f = Vector3f(),
    val radius: Float = 1f,
    val temperature: Float = 0f
)

internal class Grid(
    boundingVolume: AABBfc,
    cellSize: Vector3fc,
) {
    private val gridSize: Vector3ic = (boundingVolume.extent(Vector3f()) / cellSize).floor().toInt()
    private val grid: List<List<FireParticle>> = listOf()

    internal val neighbours = Neighbours()

    fun setParticles(particles: List<FireParticle>) {
    }

    operator fun get(x: Int, y: Int) = grid[y * gridSize.x() + x]

    inner class Neighbours {
        operator fun get(x: Int, y: Int): List<FireParticle> = TODO()
    }
}

class FireParticleContainer(
    private val numParticles: Int,
    private val particleSize: Float,
    private val boundingVolume: AABBfc,
) {
    val particles = mutableListOf<FireParticle>()

    fun spawn() {
        if (particles.size >= numParticles) return
        particles.add(
            FireParticle(
                Vector3f(
//            cos(particles.size.toDouble() * 10f / numParticles).toFloat() / 50f,
//            -0.5f,
//            sin(particles.size.toDouble() * 10f / numParticles).toFloat() / 50f
                    -0.01f
                ), radius = particleSize
            )
        )
    }

    fun update(delta: Double) {
        val gravity = Vector3f(0f, -10f, 0f)

        particles.forEachIndexed { index, it ->
            if (index == 0) println(
                "Pos: ${it.position}, prev pos: ${it.prevPosition}, damping: ${
                ((it.prevPosition - it.position) * 0.1f).apply {
                    x = x.coerceAtLeast(0f); y = y.coerceAtLeast(0f); z = z.coerceAtLeast(0f)
                }
            }")

            val newPosition = (it.position * 2f - it.prevPosition) + (
                    gravity - ((it.prevPosition - it.position) * 0.1f).apply {
                        x = x.coerceAtLeast(0f); y = y.coerceAtLeast(0f); z = z.coerceAtLeast(0f)
                    }) * (delta * delta).toFloat()

            it.prevPosition.set(it.position)
            it.position.set(newPosition)

            particles.forEach { other ->
                if (other == it) return@forEach
                val distance = it.position - other.position
                val depth = it.radius + other.radius - distance.length()
                println(it.position)
                if (index == 0) println("Distance: $distance, radius: ${it.radius}, other radius: ${other.radius}, depth: $depth")
                if (depth <= 0f) return@forEach

                val normal = if (distance.length() == 0f) Vector3f(0f) else distance.normalize()
                it.position.sub(normal * depth / 2f)
                it.prevPosition.sub(normal * depth / 2f)
                other.position.add(normal * depth / 2f)
                other.prevPosition.add(normal * depth / 2f)
            }

            val xOverlap = Math.clamp(it.position.x + it.radius - boundingVolume.minX(), Float.NEGATIVE_INFINITY, 0f) +
                    Math.clamp(it.position.x + it.radius - boundingVolume.maxX(), 0f, Float.POSITIVE_INFINITY)
            val yOverlap = Math.clamp(it.position.y + it.radius - boundingVolume.minY(), Float.NEGATIVE_INFINITY, 0f) +
                    Math.clamp(it.position.y + it.radius - boundingVolume.maxY(), 0f, Float.POSITIVE_INFINITY)
            val zOverlap = Math.clamp(it.position.z + it.radius - boundingVolume.minZ(), Float.NEGATIVE_INFINITY, 0f) +
                    Math.clamp(it.position.z + it.radius - boundingVolume.maxZ(), 0f, Float.POSITIVE_INFINITY)

            it.position.sub(xOverlap, yOverlap, zOverlap)
//            it.prevPosition.sub(xOverlap, yOverlap, zOverlap)
            it.prevPosition.set(
                if (xOverlap > 0f) it.position.x else it.prevPosition.x - xOverlap,
                if (yOverlap > 0f) it.position.y else it.prevPosition.y - yOverlap,
                if (zOverlap > 0f) it.position.z else it.prevPosition.z - zOverlap
            )

            if (index == 0) println("Overlap: ($xOverlap, $yOverlap, $zOverlap)")

            //FIXME prev position y component is not properly overwritten somewhere, fucking with particle on particle collision
        }
    }
}

fun Vector3fc.toInt() = Vector3i(x().toInt(), y().toInt(), z().toInt())

//---------------------------------------------------------------------

fun main() = FireSim.run()

object FireSim : App() {
    const val NUM_PARTICLES = 2
    const val PARTICLE_SIZE = 0.1f
    val container = FireParticleContainer(NUM_PARTICLES, PARTICLE_SIZE, AABBf(-1f, 0f, -1f, 1f, 5f, 1f))
    val emitter = ParticleEmitter(numParticles = NUM_PARTICLES, size = 2f * PARTICLE_SIZE)

    override fun init() {
        val camera = PerspectiveCamera(window.currentSize).apply {
            setOrbit(true)
            setOrbitDistance(5f)
            setRotation(-45f, 45f, 0f)
        }

        entitySystem.addService(RenderService(renderer, camera, window.currentSize).apply { blur(false) })

        entitySystem.createEntity()
            .withComponent(Transform())
            .withComponent(ParticleNode(listOf(emitter)))
    }

    private var lastTime = pacer.time
    override fun loop(delta: Double) {
        for (particle in container.particles) {
            println(particle.position)
        }

        if (pacer.time - lastTime - 0.1f > 0) {
            container.spawn()
            lastTime = pacer.time
        }

        container.update(delta)

        emitter.setParticles(container.particles) { fireParticle, particle ->
            particle.position.set(fireParticle.position)
            particle.colour.set(Vector4f(1f, 0f, 0f, 1f))
        }
    }
}
