package org.etieskrill.injection.particle.generation;

import org.etieskrill.injection.math.Vector2;

public class ConstantPositionStrategy implements PositionStrategy {

    private final Vector2 position;

    public ConstantPositionStrategy(Vector2 position) {
        this.position = position;
    }

    //TODO make Vector2 immutable?
    @Override
    public Vector2 get() {
        return position;
    }

}
