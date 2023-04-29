package org.etieskrill.injection.particle.generation;

import org.etieskrill.injection.math.Vector2f;

public class ConstantVelocityStrategy implements VelocityStrategy {

    private final Vector2f velocity;

    public ConstantVelocityStrategy(Vector2f velocity) {
        this.velocity = velocity;
    }

    @Override
    public Vector2f get() {
        return velocity;
    }

}
