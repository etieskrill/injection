package org.etieskrill.injection.particle.generation;

import org.etieskrill.injection.math.Vector2;

public class ConstantVelocityStrategy implements VelocityStrategy {

    private final Vector2 velocity;

    public ConstantVelocityStrategy(Vector2 velocity) {
        this.velocity = velocity;
    }

    @Override
    public Vector2 get() {
        return velocity;
    }

}
