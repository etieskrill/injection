package org.etieskrill.injection;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This is both a particle container and a physics solver engine as of now, and further abstraction will be introduced
 * as necessary.
 */
public class PhysicsContainer {

    private static final float minSize = 7f, maxSize = 7f;
    private static final Vector2 initPosPrev = new Vector2(150f, 100f);
    private static final Vector2 initPos = new Vector2(initPosPrev.getX() + 4/* + 6*/, initPosPrev.getY());
    private static final Vector2 gravity = new Vector2(0f, 400f);

    private final Queue<Particle> particles;

    public PhysicsContainer(int particleAmount) {
        this.particles = new ConcurrentLinkedQueue<>();
        Thread spawnerThread = new Thread(() -> {
            final Random random = new Random();
            for (int i = 0; i < particleAmount; i++) {
                particles.add(new Particle(minSize < maxSize ? random.nextFloat(minSize, maxSize) : minSize, initPosPrev, initPos));
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    System.err.println("SpawnerThread could not sleep: " + e.getMessage());
                }
            }
        });
        spawnerThread.start();
    }

    public void update(float delta) {
        for (Particle particle : particles) {
            //Apply all forces, regardless of delta time
            //applyGlobalGravity(particle);
            //applyPointGravity(particle);
            applyAttraction(particle);

            //Apply all constraints after particle acted on its own accord
            //applyCircleConstraint(particle);
            applyContainerConstraint(particle);

            //Prompt particle to update, taking delta time into account
            particle.update(delta);
        }
        for (int i = 0; i < 4; i++) {
            for (Particle particle : particles) {
                solveCollisions(particle);
            }
        }
    }

    private void applyGlobalGravity(Particle particle) {
        particle.accelerate(gravity);
    }

    private final Vector2 circlePos = new Vector2(200f, 200f);
    private final float circleRadius = 200f;

    private void applyCircleConstraint(Particle particle) {
        Vector2 toParticle = particle.getPos().sub(circlePos);
        float dist = toParticle.length();

        if (dist > circleRadius - particle.getRadius()) {
            Vector2 n = toParticle.scl(1f / dist);
            Vector2 newPos = circlePos.add(n.scl(dist - particle.getRadius() / 5f));
            System.out.println("Correction vector: " + newPos.sub(particle.getPos()));
            particle.setPos(newPos);
        }
    }

    private void applyContainerConstraint(Particle particle) {
        final float width = 800f;
        final float height = 500f;

        float x = particle.getPos().getX();
        float y = particle.getPos().getY();
        float radius = particle.getRadius();

        if (y > height - radius) {
            y = height - radius;
        } else if (y < 0f + radius) {
            y = 0f + radius;
        }

        if (x > width - radius) {
            x = width - radius;
        } else if (x < 0f + radius) {
            x = 0f + radius;
        }

        particle.setPos(new Vector2(x, y));
    }

    private void applyPointGravity(Particle particle) {
        final Vector2 centreOfGravity = new Vector2(250f, 250f);
        Vector2 toParticle = centreOfGravity.sub(particle.getPos());
        float mag = toParticle.length();
        float scl = Math.min(50000000 / (mag * mag), 1000f);
        particle.accelerate(toParticle.normal().scl(scl));
    }

    private void applyAttraction(Particle particle1) {
        for (Particle particle2 : particles) {
            if (particle1.equals(particle2)) continue;
            Vector2 toParticle = particle2.getPos().sub(particle1.getPos());
            float mag = toParticle.length();
            float scl = Math.min(5000000 / (mag * mag), 2000f);
            particle1.accelerate(toParticle.normal().scl(scl));
        }
    }

    /**
     * Shitty O(n^2) algorithm for solving collisions by moving both colliding objects apart from each other
     * perpendicular to the collision axis by half the collision depth.
     */
    private void solveCollisions(Particle particle1) {
        for (Particle particle2 : particles) {
            if (particle1.equals(particle2)) continue;
            Vector2 pos = particle1.getPos().sub(particle2.getPos());
            float dist = pos.length();
            float desiredDist = particle1.getRadius() + particle2.getRadius();
            if (dist < desiredDist) {
                Vector2 normalPos = pos.normal();
                float correct = desiredDist - dist;
                particle1.setPos(particle1.getPos().add(normalPos.scl(correct)));
                particle2.setPos(particle2.getPos().add(normalPos.scl(-correct)));
            }
        }
    }

    public Queue<Particle> getParticles() {
        return particles;
    }

}
