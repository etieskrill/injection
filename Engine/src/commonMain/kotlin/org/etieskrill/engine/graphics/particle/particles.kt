package org.etieskrill.engine.graphics.particle

import net.bifreus.kml.Mat2
import net.bifreus.kml.PI_f
import net.bifreus.kml.Vec3
import net.bifreus.kml.Vec3c
import net.bifreus.kml.Vec4
import net.bifreus.kml.Vec4c
import net.bifreus.kml.lerp
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.component.TransformC
import org.etieskrill.engine.graphics.texture.Texture2D
import java.lang.Math.random
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sign
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class Particle(
    val position: Vec3 = Vec3(),
    internal val relativePosition: Vec3 = Vec3(),

    val velocity: Vec3 = Vec3(),
    var angularVelocity: Float = 0f,

    val transform: Mat2 = Mat2(),

    internal val baseColour: Vec4 = Vec4(1f),
    val colour: Vec4 = Vec4(1f),

    initialLifetimeSeconds: Float = 0f,
    var lifetimeSeconds: Float = 0f
) {
    var initialLifetimeSeconds: Float = initialLifetimeSeconds; internal set

    fun update(delta: Float, emitter: ParticleEmitter) {
        lifetimeSeconds -= delta

        relativePosition(relativePosition + velocity * delta)
        position(relativePosition)
        if (emitter.particlesMoveWithEmitter)
            position(position + emitter.transform.position)

        transform.rotate(angularVelocity * delta)

        emitter.colourUpdate(lifetimeSeconds / initialLifetimeSeconds, colour(baseColour))
    }

}

class ParticleEmitter(
    internal val transform: Transform = Transform(),
    internal val particlesMoveWithEmitter: Boolean = false,

    private val lifetime: Duration = 5.seconds,
    private val lifetimeSpread: Duration = 0.seconds,

    private val baseColour: Vec4c = Vec4(1f),
    internal val size: Float = 1f,
    internal val sprite: Texture2D? = null,

    //these update functions do not expect a new returned object (as it would be idiomatic to do), but
    //rather pass an output object to update by reference. this is to lighten the load on the gc, as these
    //emitters may create several thousand objects for each update and render call otherwise.
    initialVelocity: ((outVelocity: Vec3) -> Unit)? = null,
    randomVelocity: Float? = null,
    private val scatter: Vec3c = Vec3(0f),
    private val scatterAngleRange: ClosedFloatingPointRange<Float> = 0f..PI_f,
    private val rotationsPerSecond: Float = 0f,
    private val rotationSpread: Float = abs(rotationsPerSecond),

    particlesPerSecond: Float? = null,
    numParticles: Int? = null,
    particleSpawnDelay: Duration? = null,
    private val particleSpawnDelaySpread: Duration = 0.milliseconds,

    /**
     * Passes the normalised `lifetime` starting at `1.0` and progressing towards `0.0` for a particle at the end of
     * its lifespan, as well as the output colour vector holding the current composited colour, which can be adjusted
     * based on the remaining lifespan.
     */
    internal val colourUpdate: (lifetime: Float, outColour: Vec4) -> Unit = { lifetime, outColour ->
        outColour.w = lifetime
    },

    var spawnParticles: Boolean = true,
) {

    private val initialVelocity: (outVelocity: Vec3) -> Unit = when {
        initialVelocity != null && randomVelocity != null -> error("initialVelocity and randomVelocity may not be set simultaneously")
        initialVelocity != null -> initialVelocity
        randomVelocity != null -> { outVelocity ->
            outVelocity(Vec3(random(), random(), random()) * randomVelocity - (randomVelocity - 2f))
        }

        else -> { outVelocity -> outVelocity(0f) }
    }

    val maxNumParticles: Int = when {
        particlesPerSecond != null ->
            ceil((lifetime + lifetimeSpread + particleSpawnDelaySpread).toSeconds() * particlesPerSecond).toInt()

        numParticles != null -> numParticles
        else -> 100
    }

    val particleSpawnDelay: Duration = when {
        particleSpawnDelay != null -> particleSpawnDelay
        particlesPerSecond != null -> (1.0 / particlesPerSecond.toDouble()).seconds
        else -> 0.seconds
    }

    private val particles = List(maxNumParticles) { Particle() }
    private val internalAliveParticles = ArrayDeque<Particle>()
    val aliveParticles: List<Particle> get() = internalAliveParticles
    private var nextParticle = 0
    private var secondsSinceLastParticle = 0f

    init {
        check(lifetime >= Duration.ZERO) { "Particle lifetime must be non-negative" }
        check(size >= 0) { "Particle size must be non-negative" }
    }

    fun update(delta: Double, transform: TransformC) {
        internalAliveParticles
            .onEach { it.update(delta.toFloat(), this) }
            .removeAll { it.lifetimeSeconds <= 0 }

        if (spawnParticles) secondsSinceLastParticle += delta.toFloat()
        else return

        val localTransform = transform * this.transform

        val nextParticleDelay =
            particleSpawnDelay.toSeconds() + (random() * 2 - 1) * particleSpawnDelaySpread.toSeconds()
        while (secondsSinceLastParticle >= nextParticleDelay
            && particles[nextParticle].lifetimeSeconds <= 0
        ) {
            if (spawnParticles) particles[nextParticle].apply {
                if (particlesMoveWithEmitter) relativePosition(0)
                else relativePosition(localTransform.position)

                if (scatter != Vec3.ZERO) {
                    relativePosition(
                        relativePosition
                                + (
                                Vec3(random().toFloat(), random().toFloat(), random().toFloat())
                                        * 2 * scatter
                                ) - scatter
                    )
                }

                baseColour(this@ParticleEmitter.baseColour)

                initialVelocity(velocity)
                velocity(transform.matrix.timesDirection(velocity))

                this.transform(1).rotate(scatterAngleRange.lerp(random().toFloat()))
                val rotationSpread = rotationSpread * (random().toFloat() * 2 - 1)
                angularVelocity = (2 * PI_f * (rotationsPerSecond + rotationSpread) * sign(random() - 0.5).toFloat())

                initialLifetimeSeconds = lifetime.toSeconds().toFloat() + lerp(
                    -lifetimeSpread.toSeconds().toFloat(),
                    lifetimeSpread.toSeconds().toFloat(),
                    random().toFloat()
                )
                lifetimeSeconds = initialLifetimeSeconds

                update(0f, this@ParticleEmitter)
                internalAliveParticles += this
                nextParticle = ++nextParticle % particles.size
            }

            secondsSinceLastParticle -= particleSpawnDelay.toSeconds().toFloat()
        }
    }

    fun <T : Any> setParticles(source: List<T>, block: (source: T, target: Particle) -> Unit) {
        check(source.size <= maxNumParticles)
        { "Source contains too many entries (${source.size}) for emitter with max particle count of $maxNumParticles" }

        internalAliveParticles.clear()
        source.zip(particles).forEach {
            block(it.first, it.second)
            internalAliveParticles.add(it.second)
        }
    }

}

private fun Duration.toSeconds() = toDouble(DurationUnit.SECONDS)
//private fun ClosedRange<Float>.lerp(t: Float) = start + t * (endInclusive - start)

//private operator fun Vector3f.plusAssign(other: Vector3fc) {
//    add(other)
//}

class ParticleNode(
    val emitters: List<ParticleEmitter> = listOf(),
    val children: List<ParticleNode> = listOf(),
    val transform: Transform = Transform()
) {

    fun update(delta: Double) {
        update(delta, Transform())
    }

    private fun update(delta: Double, transform: TransformC) {
        val transform = transform * this.transform
        emitters.forEach { it.update(delta, transform) }
        children.forEach { it.update(delta, Transform(transform)) }
    }

    fun setSpawnParticles(spawnParticles: Boolean) {
        emitters.forEach { it.spawnParticles = spawnParticles }
        children.forEach { it.setSpawnParticles(spawnParticles) }
    }

}
