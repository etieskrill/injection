package org.etieskrill.engine.graphics.particle;

import lombok.Builder;
import lombok.Getter;
import org.etieskrill.engine.common.Interpolator;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.jetbrains.annotations.Range;
import org.joml.Math;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import static java.lang.Math.clamp;
import static java.util.Objects.requireNonNullElse;
import static lombok.AccessLevel.PACKAGE;
import static org.joml.Math.*;

/**
 * A particle emitter keeps track of a set of {@link Particle Particles}, which are spawned at a semi-fixed interval.
 * <p>
 * Various parameters define a particle's behaviour, including lifetime, starting position, starting angle, velocity,
 * angular velocity, base colour and texture.
 * <p>
 * Additionally, a custom {@link UpdateFunction} may be passed in order to update the colour of a particle based on its
 * lifetime. By default, this function decreases the particle colour's alpha value linearly from 1 to 0 over the
 * particle's lifetime.
 * <p>
 * If particles are spawned more quickly than their lifetime expires, the particle pool will experience starvation, as
 * new particles wait for a free particle slot instead of eagerly overwriting existing ones.
 */
public class ParticleEmitter {

    private final @Getter Transform transform;
    private final @Getter boolean particlesMoveWithEmitter;
    private final Supplier<Vector3f> velocity;
    private final @Getter float lifetime;
    private final float lifetimeSpread;
    private final Vector4f colour;
    private final @Getter float size;
    private final @Getter Texture2D sprite;

    @Range(from = 0, to = Integer.MAX_VALUE)
    private final Vector3f scatter;
    @Range(from = 0, to = 360)
    private final float minScatterAngle, maxScatterAngle;
    private final float revolutionsPerSecond;
    private final float revolutionSpread;

    private final float particleDelaySeconds;
    private final float particleDelaySpreadSeconds;

    private final @Getter(PACKAGE) UpdateFunction updateFunction;

    private final @Getter int maxNumParticles;
    private final List<Particle> particles;
    private final @Getter ArrayDeque<Particle> aliveParticles;

    private int nextParticle;
    private float secondsSinceLastParticle;

    private final Random random = new Random();

    @FunctionalInterface
    public interface UpdateFunction {
        void update(float normalisedLifetime, Vector4f colour); //TODO generalise over other params?
    }

    public static ParticleEmitterBuilder builder(float lifetime, Texture2D sprite) {
        return create()
                .lifetime(lifetime)
                .sprite(sprite);
    }

    public static class ParticleEmitterBuilder {
        private UpdateFunction updateFunction = (lifetime, colour) -> {
            colour.w *= Interpolator.LINEAR.interpolate(lifetime);
        };

        public ParticleEmitterBuilder velocity(Supplier<Vector3f> velocity) {
            this.velocity = velocity;
            return this;
        }

        public ParticleEmitterBuilder velocity(Vector3f velocity) {
            this.velocity = () -> velocity;
            return this;
        }

        public ParticleEmitterBuilder randomVelocity(float scalar) {
            this.velocity = () -> new Vector3f(
                    (float) Math.random(),
                    (float) Math.random(),
                    (float) Math.random()
            ).mul(scalar).sub(scalar / 2, scalar / 2, scalar / 2);
            return this;
        }

        public ParticleEmitterBuilder scatter(Vector3f scatter) {
            this.scatter = scatter;
            return this;
        }

        public ParticleEmitterBuilder scatter(float x, float y, float z) {
            this.scatter = new Vector3f(x, y, z);
            return this;
        }

        public ParticleEmitterBuilder colour(Vector4f colour) {
            this.colour = colour;
            return this;
        }

        public ParticleEmitterBuilder colour(float r, float g, float b, float a) {
            this.colour = new Vector4f(r, g, b, a);
            return this;
        }

        public ParticleEmitterBuilder colour(float r, float g, float b) {
            this.colour = new Vector4f(r, g, b, 1);
            return this;
        }

        public ParticleEmitterBuilder updateFunction(UpdateFunction updateFunction) {
            this.updateFunction = updateFunction;
            return this;
        }

        public ParticleEmitterBuilder updateAlphaFunction(Interpolator interpolator) {
            this.updateFunction = (lifetime, colour) -> colour.w *= interpolator.interpolate(lifetime);
            return this;
        }
    }

    @Builder(builderMethodName = "create")
    private ParticleEmitter(
            Transform transform,
            boolean particlesMoveWithEmitter,
            Supplier<Vector3f> velocity,
            float lifetime,
            float lifetimeSpread,
            Vector4f colour,
            float size,
            Texture2D sprite,
            Vector3f scatter,
            int numParticles,
            float particlesPerSecond,
            float particleDelaySpreadSeconds,
            float minScatterAngle,
            float maxScatterAngle,
            float revolutionsPerSecond,
            float revolutionSpread,
            UpdateFunction updateFunction
    ) {
        this.transform = requireNonNullElse(transform, new Transform());
        this.particlesMoveWithEmitter = particlesMoveWithEmitter;
        this.velocity = requireNonNullElse(velocity, () -> new Vector3f(0));
        this.lifetime = lifetime;
        this.lifetimeSpread = abs(lifetimeSpread);
        this.colour = requireNonNullElse(colour, new Vector4f(1));
        this.size = size > 0 ? size : 1;
        this.sprite = sprite;

        this.scatter = requireNonNullElse(scatter, new Vector3f(0));

        this.particleDelaySeconds = 1f / particlesPerSecond;
        this.particleDelaySpreadSeconds = abs(particleDelaySpreadSeconds);

        this.minScatterAngle = clamp(minScatterAngle, 0, 360);
        this.maxScatterAngle = clamp(maxScatterAngle, 0, 360);
        this.revolutionsPerSecond = revolutionsPerSecond;
        this.revolutionSpread = abs(revolutionSpread);

        this.updateFunction = updateFunction;

        if (numParticles <= 0) {
            numParticles = (int) ceil((lifetime + lifetimeSpread + particleDelaySpreadSeconds) * particlesPerSecond);
        }

        this.maxNumParticles = numParticles;
        this.particles = new ArrayList<>(numParticles);
        for (int i = 0; i < numParticles; i++) {
            this.particles.add(new Particle());
        }
        this.aliveParticles = new ArrayDeque<>(numParticles);
        this.nextParticle = 0;
        this.secondsSinceLastParticle = 0;
    }

    public void update(double delta) {
        aliveParticles.forEach(particle -> particle.update((float) delta, this));

        while (!aliveParticles.isEmpty() && aliveParticles.getLast().getLifetime() <= 0) {
            aliveParticles.removeLast();
        }

        secondsSinceLastParticle += delta;

        float nextParticleDelay = particleDelaySeconds + (random.nextFloat() * 2 - 1) * particleDelaySpreadSeconds;
        while (secondsSinceLastParticle >= nextParticleDelay) {
            if (particles.get(nextParticle).getLifetime() <= 0) { //TODO maybe override particles instead of waiting
                Particle spawnedParticle = particles.get(nextParticle);
                spawnedParticle.withPosition(position -> {
                    if (particlesMoveWithEmitter) position.zero();
                    else position.set(transform.getPosition());
                });
                if (scatter.x() != 0 || scatter.y() != 0 || scatter.z() != 0) spawnedParticle.withPosition(position ->
                        position.add(
                                2 * random.nextFloat(scatter.x()),
                                2 * random.nextFloat(scatter.y()),
                                2 * random.nextFloat(scatter.z())
                        ).sub(scatter));
                spawnedParticle.getVelocity().set(transform.getMatrix().transformDirection(velocity.get()));
                spawnedParticle.getBaseColour().set(colour);
                spawnedParticle.getTransform()
                        .rotation(toRadians(lerp(minScatterAngle, maxScatterAngle, random.nextFloat())));
                float revSpread = revolutionSpread * (random.nextFloat() * 2 - 1);
                spawnedParticle.setAngularVelocity(
                        (float) (2 * PI * (revolutionsPerSecond + revSpread) * (random.nextBoolean() ? 1 : -1))
                );
                spawnedParticle.setLifetime(lifetime + lerp(-lifetimeSpread, lifetimeSpread, random.nextFloat()));
                spawnedParticle.update(0, this);
                aliveParticles.push(spawnedParticle);

                nextParticle = ++nextParticle % particles.size();
            }
            secondsSinceLastParticle -= particleDelaySeconds;
        }
    }

}
