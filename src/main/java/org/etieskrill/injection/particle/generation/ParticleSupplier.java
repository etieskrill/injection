package org.etieskrill.injection.particle.generation;

import org.etieskrill.injection.math.Vector2f;
import org.etieskrill.injection.particle.Particle;

import java.util.function.Supplier;

public final class ParticleSupplier implements Supplier<Particle> {

    private final SizeStrategy sizeStrategy;
    private final PositionStrategy positionStrategy;
    private final VelocityStrategy velocityStrategy;

    public ParticleSupplier(SizeStrategy sizeStrategy, PositionStrategy positionStrategy, VelocityStrategy velocityStrategy) {
        this.sizeStrategy = sizeStrategy;
        this.positionStrategy = positionStrategy;
        this.velocityStrategy = velocityStrategy;
    }

    @Override
    public Particle get() {
        Vector2f pos = positionStrategy.get();
        return new Particle(sizeStrategy.get(), pos, pos.add(velocityStrategy.get()));
    }

}
