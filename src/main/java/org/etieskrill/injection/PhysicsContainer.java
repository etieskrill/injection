package org.etieskrill.injection;

import org.etieskrill.injection.math.Interpolator;
import org.etieskrill.injection.math.Vector2;
import org.etieskrill.injection.particle.Particle;
import org.etieskrill.injection.particle.generation.*;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.etieskrill.injection.CLPhysicsContainer.checkCLError;
import static org.lwjgl.opencl.CL10.clWaitForEvents;

/**
 * This is both a particle container and a physics solver engine as of now, and further abstraction will be introduced
 * as necessary.
 */
public class PhysicsContainer {

    private static final Vector2 gravity = new Vector2(0f, 400f);
    
    private final int particleAmount = 1000;
    private final Queue<Particle> particles;
    
    private final CLPhysicsContainer clContainer;

    public PhysicsContainer() {
        this.particles = new ConcurrentLinkedQueue<>();
        ParticleSupplier supplier = new ParticleSupplier(
                new ConstantSizeStrategy(3f),
                new ConstantPositionStrategy(new Vector2(50f, 100f)),
                new ConstantVelocityStrategy(new Vector2(0.5f, 0f))
                /*new SwivelingVelocityStrategy(new Vector2(2f, 0f), new Vector2(0f, 2f),
                        70, Interpolator.Interpolation.LINEAR, true)*/
        );
        ParticleSupplier supplier2 = new ParticleSupplier(
                new ConstantSizeStrategy(3f),
                new ConstantPositionStrategy(new Vector2(50f, 50f)),
                new ConstantVelocityStrategy(new Vector2(0.5f, 0f))
                /*new SwivelingVelocityStrategy(new Vector2(2f, 0f), new Vector2(0f, 2f),
                        70, Interpolator.Interpolation.LINEAR, true)*/
        );
        this.clContainer = new CLPhysicsContainer(particles, particleAmount);
        new ParticleSpawner(particleAmount / 2, supplier, particles, 30).start();
        new ParticleSpawner(particleAmount / 2, supplier2, particles, 30).start();
    }
    
    private final float subSteps = 64;
    
    /*protected void update(float delta) {
        float subDelta = delta / subSteps;
        
        for (int i = 0; i < subSteps; i++) {
            for (Particle particle : particles) {
                //Apply all forces, regardless of delta time
                applyGlobalGravity(particle);
                //applyPointGravity(particle);
                //applyAttraction(particle);
                //applyHeating(particle);
                //applyHeatSpread(particle);
                //applyHeatForce(particle);
    
                //Apply all constraints after particle acted on its own accord
                //applyCircleConstraint(particle);
                applyContainerConstraint(particle);
                solveCollisions(particle);
    
                //Prompt particle to update, taking delta time into account
                particle.update(subDelta);
            }
        }
    }*/
    
    protected void update(float delta) {
        float subDelta = delta / subSteps;
        int size = particles.size();
        clContainer.setPosBuffer();
        
        for (int i = 0; i < subSteps; i++) {
            clContainer.updatePosBuffer(subDelta);
        }
        
        //Do not update particles added since the last call to setPosBuffer
        clContainer.pollPosBuffer(particles, size);
    }

    private void applyGlobalGravity(Particle particle) {
        particle.accelerate(gravity);
    }
    
    private void applyHeating(Particle particle) {
        final float height = App.windowSize.getY();
    
        float x = particle.getPos().getX();
        float y = particle.getPos().getY();
        float radius = particle.getRadius();
        float tolerance = 2 * radius;
        float center = 600f;
        float size = 600;
        
        float dist = Math.abs(x - center);
    
        if (y > height - radius - tolerance && dist < size) {
            particle.heat(0.5f / subSteps);
        }
    }
    
    private void applyHeatSpread(Particle particle1) {
        for (Particle particle2 : particles) {
            if (particle1.equals(particle2)) continue;
            float dist = particle1.getPos().sub(particle2.getPos()).length();
            /*if (dist > 2 * (particle1.getRadius() + particle2.getRadius())) {
                particle1.heat(particle2.getTemp() / (3f * dist * dist));
            }*/
            float transfer = particle1.getTemp() / (4f * dist * dist);
            particle1.heat(-transfer);
            particle2.heat(transfer);
        }
    }
    
    private final Vector2 heatForce = new Vector2(0f, -2000f);
    
    private void applyHeatForce(Particle particle) {
        float temp = particle.getTemp();
        particle.accelerate(heatForce.scl(temp));
    }

    private final Vector2 circlePos = new Vector2(200f, 200f);
    private final float circleRadius = 200f;

    private void applyCircleConstraint(Particle particle) {
        Vector2 toParticle = particle.getPos().sub(circlePos);
        float dist = toParticle.length();

        if (dist > circleRadius - particle.getRadius()) {
            Vector2 n = toParticle.scl(1f / dist);
            Vector2 newPos = circlePos.add(n.scl(dist - particle.getRadius() / 10f));
            particle.setPos(newPos);
        }
    }

    private void applyContainerConstraint(Particle particle) {
        final float width = App.windowSize.getX();
        final float height = App.windowSize.getY();

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
        final Vector2 centreOfGravity = new Vector2(App.windowSize.getX() / 2f, App.windowSize.getY() / 2f);
        Vector2 toParticle = centreOfGravity.sub(particle.getPos());
        float mag = toParticle.length();
        float scl = Math.min(50000000 / (mag * mag), 2000f);
        particle.accelerate(toParticle.normal().scl(scl));
    }

    private void applyAttraction(Particle particle1) {
        for (Particle particle2 : particles) {
            if (particle1.equals(particle2)) continue;
            Vector2 toParticle = particle2.getPos().sub(particle1.getPos());
            float mag = toParticle.length();
            float scl = Math.min(5000000 / (mag * mag), 5f);
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
                float correct = (desiredDist - dist) / 2f;
                particle1.setPos(particle1.getPos().add(normalPos.scl(correct)));
                particle2.setPos(particle2.getPos().add(normalPos.scl(-correct)));
            }
        }
    }

    public Queue<Particle> getParticles() {
        return particles;
    }

}
