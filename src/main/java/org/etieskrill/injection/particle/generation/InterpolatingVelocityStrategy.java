package org.etieskrill.injection.particle.generation;

import org.etieskrill.injection.math.Interpolator;
import org.etieskrill.injection.math.Vector2f;

public class InterpolatingVelocityStrategy implements VelocityStrategy {

    private final Vector2f start;
    private final Vector2f end;
    private final int cycleLength;
    private final Interpolator interpolator;

    private int cycle = 0;

    public InterpolatingVelocityStrategy(Vector2f start, Vector2f end, int cycleLength,
                                         Interpolator.Interpolation interpolation, boolean bounce) {
        this.start = start;
        this.end = end;
        this.cycleLength = cycleLength;
        this.interpolator = Interpolator.getFor(interpolation, bounce, 0f, cycleLength);
    }

    @Override
    public Vector2f get() {
        cycle %= cycleLength;
        return end.interpolate(start, interpolator.get(cycle++));
    }

}
