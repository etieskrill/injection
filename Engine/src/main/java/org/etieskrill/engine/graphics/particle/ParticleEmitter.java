package org.etieskrill.engine.graphics.particle;

import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleEmitter {

    private final Transform transform;
    private final Vector3f velocity;
    private final float lifetime;
    private final Vector4f colour;
    private final float size;
    private final Texture2D sprite;

    private final Vector3f scatter;

    private final float particleDelaySeconds;

    private final List<Particle> particles;
    private final ArrayDeque<Particle> aliveParticles;

    private int nextParticle;
    private float secondsSinceLastParticle;

    private final Random random;

    public ParticleEmitter(int numParticles,
                           int particlesPerSecond,
                           Transform transform,
                           Vector3f velocity,
                           float lifetime,
                           Vector4f colour,
                           float size,
                           Texture2D sprite,
                           Vector3f scatter) {
        this.transform = transform;
        this.velocity = velocity;
        this.lifetime = lifetime;
        this.colour = colour;
        this.size = size;
        this.sprite = sprite;

        this.scatter = scatter;

        this.particleDelaySeconds = 1f / particlesPerSecond;

        this.particles = new ArrayList<>(numParticles);
        for (int i = 0; i < numParticles; i++) {
            this.particles.add(new Particle());
        }
        this.aliveParticles = new ArrayDeque<>(numParticles);
        this.nextParticle = 0;
        this.secondsSinceLastParticle = 0;

        this.random = new Random();
    }

    public void update(double delta) {
        aliveParticles.forEach(particle -> particle.update((float) delta));

        while (!aliveParticles.isEmpty() && aliveParticles.getLast().getLifetime() <= 0) {
            aliveParticles.removeLast();
        }

        secondsSinceLastParticle += delta;

        while (secondsSinceLastParticle >= particleDelaySeconds) {
            if (particles.get(nextParticle).getLifetime() <= 0) { //TODO maybe override particles instead of waiting
                Particle spawnedParticle = particles.get(nextParticle);
                spawnedParticle.getPosition().set(transform.getPosition());
                spawnedParticle.getPosition().add(
                        2 * random.nextFloat(scatter.x()),
                        2 * random.nextFloat(scatter.y()),
                        2 * random.nextFloat(scatter.z())
                ).sub(scatter);
                spawnedParticle.getVelocity().set(transform.getMatrix().transformPosition(velocity));
                spawnedParticle.getColour().set(colour);
                spawnedParticle.setLifetime(lifetime);
                aliveParticles.push(spawnedParticle);

                nextParticle = ++nextParticle % particles.size();
            }
            secondsSinceLastParticle -= particleDelaySeconds;
        }
    }

    public float getSize() {
        return size;
    }

    public Texture2D getSprite() {
        return sprite;
    }

    public ArrayDeque<Particle> getAliveParticles() {
        return aliveParticles;
    }

}
