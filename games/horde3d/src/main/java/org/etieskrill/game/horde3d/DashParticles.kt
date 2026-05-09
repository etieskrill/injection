package org.etieskrill.game.horde3d

import org.etieskrill.engine.common.Interpolator
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.graphics.particle.ParticleEmitter
import org.etieskrill.engine.graphics.particle.ParticleNode
import org.etieskrill.engine.graphics.texture.AbstractTexture
import org.etieskrill.engine.graphics.texture.Texture2D
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.time.Duration.Companion.seconds

class DashParticles {

    val particles: ParticleNode

    init {
        val riftSmokeEmitter = ParticleEmitter(
            lifetime = 1.seconds,
            sprite = Texture2D.FileBuilder("textures/particles/smoke_05.png")
                .setWrapping(AbstractTexture.Wrapping.CLAMP_TO_BORDER).build(),
            particlesPerSecond = 500f,
            randomVelocity = 2f,
            baseColour = Vector4f(0.005f, 0f, 0.043f, 1f),
            colourUpdate = { lifetime, outColour -> outColour.w = Interpolator.QUADRATIC.interpolate(lifetime) },
            scatter = Vector3f(0.5f, 1.5f, 0.5f),
        )

        val riftSparkEmitter = ParticleEmitter(
            lifetime = 0.5.seconds,
            sprite = Texture2D.FileBuilder("textures/particles/spark_04.png")
                .setWrapping(AbstractTexture.Wrapping.CLAMP_TO_BORDER).build(),
            particlesPerSecond = 20f,
            particleSpawnDelaySpread = 0.25.seconds,
            baseColour = Vector4f(8f, 5f, 8f, 1f),
            size = 1.5f,
            colourUpdate = { lifetime, outColour ->
                val t = (2 * lifetime) % 1f
                outColour.w = when {
                    t < 0.1 -> 0f
                    t < 0.5 -> 0.1f
                    t < 0.55 -> 1f
                    else -> 0f
                }
            },
            scatter = Vector3f(0.5f, 1.5f, 0.5f),
            rotationsPerSecond = 0.1f,
        )

        val riftShineEmitter = ParticleEmitter(
            lifetime = 1.seconds,
            numParticles = 1,
            sprite = Texture2D.FileBuilder("textures/particles/star_01.png")
                .setWrapping(AbstractTexture.Wrapping.CLAMP_TO_BORDER).build(),
            particlesMoveWithEmitter = true,
            baseColour = Vector4f(1f, 0.75f, 1f, 1f),
            colourUpdate = { lifetime, outColour ->
                outColour.w = Interpolator.SMOOTHSTEP.interpolate(lifetime)
            },
            size = 5f,
            scatterAngleRange = 0f..0f
        )

        particles = ParticleNode(
            emitters = listOf(
                riftSmokeEmitter,
                riftSparkEmitter,
                riftShineEmitter
            ),
            transform = Transform(position = Vector3f(0f, 5f, 0f)),
        )
    }

}
