package org.etieskrill.injection.math;

import java.util.Objects;

/**
 * This is a class which provides an assortment of interpolation functions, which can be expanded upon.
 *
 * <p>At it's core lies the private {@code _get} function, which maps a normalised {0;1} input domain to an equal
 * normalised {0;1} output range. The range of the input domain can be manually defined should the need arise and is
 * normalised in the public {@code get} function.
 *
 * <p>An interpolator is instantiated via the {@code getFor} factory method, which returns a subclass of
 * {@code Interpolator} as specified by the {@code interpolation} argument.
 *
 * <p>Alternatively, custom interpolation implementations can be created by extending the {@code ExternalInterpolator}
 * class.
 *
 * @author etieskrill
 */
public abstract class Interpolator {

    private final float rangeStart;
    private final float rangeEnd;
    private final boolean bounce;

    public enum Interpolation {
        NONE,
        LINEAR,
        QUADRATIC,
        CUBIC,
        EASE_IN,
        EASE_OUT,
        EASE_IN_OUT;
    }

    private Interpolator(float rangeStart, float rangeEnd, boolean bounce) {
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.bounce = bounce;
    }

    /**
     * This class provides external access for custom interpolation implementations.
     */
    public static abstract class ExternalInterpolator extends Interpolator {
        public ExternalInterpolator(float rangeStart, float rangeEnd, boolean bounce) {
            super(rangeStart, rangeEnd, bounce);
        }
    }

    public static Interpolator getFor(Interpolation interpolation) {
        return getFor(interpolation, false, 0f, 1f);
    }

    public static Interpolator getFor(Interpolation interpolation, boolean bounce) {
        return getFor(interpolation, bounce, 0f, 1f);
    }

    public static Interpolator getFor(Interpolation interpolation, boolean bounce, float rangeStart, float rangeEnd) {
        /*switch (interpolation) { //TODO figure out how to get maven to compile this
            case LINEAR -> {
                return new Linear(rangeStart, rangeEnd, bounce);
            }
            case default -> throw new UnsupportedOperationException(interpolation.name() + " is not yet implemented");
        }*/
        if (Objects.requireNonNull(interpolation) == Interpolation.LINEAR) {
            return new Linear(rangeStart, rangeEnd, bounce);
        }
        throw new UnsupportedOperationException(interpolation.name() + " is not yet implemented");
    }

    public float get(float f) {
        if (f < rangeStart || f > rangeEnd) throw new IllegalArgumentException("Supplied value not in input range");
        float norm = (f - rangeStart) / (rangeEnd - rangeStart);
        if (bounce) norm = -Math.abs(2 * norm - 1) + 1f;
        return _get(norm); //Pass normalised and possibly bounced value to internal function
    }

    protected abstract float _get(float f);

    private static class Linear extends Interpolator {
        private Linear(float rangeStart, float rangeEnd, boolean bounce) {
            super(rangeStart, rangeEnd, bounce);
        }

        @Override
        protected float _get(float f) {
            return f;
        }
    }

}
