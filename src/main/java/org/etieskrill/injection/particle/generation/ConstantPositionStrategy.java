package org.etieskrill.injection.particle.generation;

import org.etieskrill.injection.math.Vector2f;

public class ConstantPositionStrategy implements PositionStrategy {

    private final Vector2f position;

    public ConstantPositionStrategy(Vector2f position) {
        this.position = position;
    }

    //TODO make Vector2f immutable?
    @Override
    public Vector2f get() {
        return position;
    }

}
