package org.etieskrill.injection.particle.generation;

import org.etieskrill.injection.math.Interpolator;
import org.etieskrill.injection.math.Vector2;

public class SwivelingVelocityStrategy extends InterpolatingVelocityStrategy {

    public SwivelingVelocityStrategy(Vector2 start, Vector2 end, int cycleLength,
                                     Interpolator.Interpolation interpolation, boolean bounce) {
        super(checkMagnitude(start, end), end, cycleLength, interpolation, bounce);
    }

    /*
    While this pattern for checking preconditions is well known and maintains good practice, it is a bit ugly if more
    than a single argument has to be checked, such as here. This entire class assembly screams for a factory pattern
    anyway, with which these troubles will be resolved. So, TODO turn this into factory pattern
     */
    private static Vector2 checkMagnitude(Vector2 vec1, Vector2 vec2) {
        if (vec1.length() != vec2.length()) throw new IllegalArgumentException("Magnitude of vectors does not match");
        return vec1;
    }

}
