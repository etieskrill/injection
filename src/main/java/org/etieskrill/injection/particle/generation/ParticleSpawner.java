package org.etieskrill.injection.particle.generation;

import org.etieskrill.injection.particle.Particle;

import java.util.Collection;

public class ParticleSpawner extends Thread {

    private final int particleAmount;
    private final ParticleSupplier supplier;
    private final Collection<Particle> particles;
    private final long spawnDelayMillis;

    public ParticleSpawner(int particleAmount, ParticleSupplier supplier, Collection<Particle> particles,
                              long spawnDelayMillis) {
        this.particleAmount = particleAmount;
        this.supplier = supplier;
        this.particles = particles;
        this.spawnDelayMillis = spawnDelayMillis;
    }

    @Override
    public void run() {
        for (int i = 0; i < particleAmount; i++) {
            particles.add(supplier.get());
            try {
                Thread.sleep(spawnDelayMillis);
            } catch (InterruptedException e) {
                System.err.println("SpawnerThread could not sleep:\n" + e.getMessage());
            }
        }
    }

}
