package org.etieskrill.game.horde3d;

import org.etieskrill.engine.common.Interpolator;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.graphics.particle.ParticleEmitter;
import org.etieskrill.engine.graphics.particle.ParticleNode;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class DashParticles {

    private final ParticleNode particles;

    public DashParticles() {
        ParticleEmitter riftSmokeEmitter = ParticleEmitter.builder(
                        1,
                        new Texture2D.FileBuilder("particles/smoke_05.png", AbstractTexture.Type.DIFFUSE).build())
                .particlesPerSecond(500)
                .randomVelocity(2)
                .colour(new Vector4f(.005f, 0, .043f, 1))
                .updateAlphaFunction(Interpolator.QUADRATIC)
                .scatter(new Vector3f(.5f, 1.5f, .5f))
                .build();

        ParticleEmitter riftSparkEmitter = ParticleEmitter.builder(
                        .5f,
                        new Texture2D.FileBuilder("particles/spark_04.png", AbstractTexture.Type.DIFFUSE).build())
                .lifetimeSpread(.5f)
                .particlesPerSecond(20)
                .particleDelaySpreadSeconds(.25f)
                .colour(8, 5, 8)
                .size(1.5f)
                .updateFunction((lifetime, colour) -> {
                    lifetime = (2 * lifetime) % 1f;
                    colour.w = lifetime < 0.1 ? 0 : (lifetime < 0.5 ? 0.1f : (lifetime < 0.55 ? 1 : 0));
                })
                .scatter(.5f, 1.5f, .5f)
                .maxScatterAngle(360)
                .revolutionsPerSecond(.1f)
                .build();

        ParticleEmitter riftShineEmitter = ParticleEmitter.builder(
                        1,
                        new Texture2D.FileBuilder("particles/star_01.png", AbstractTexture.Type.DIFFUSE).build())
                .particlesPerSecond(.5f)
                .particlesMoveWithEmitter(true)
                .colour(1, .75f, 1)
                .updateAlphaFunction(Interpolator.SMOOTHSTEP)
                .size(5)
                .build();

        particles = ParticleNode.builder()
                .emitter(riftSmokeEmitter)
                .emitter(riftSparkEmitter)
                .emitter(riftShineEmitter)
                .transform(new Transform().setPosition(new Vector3f(0, 5, 0)))
                .build();
    }

    public ParticleNode getParticles() {
        return particles;
    }

}
