package org.etieskrill.engine.common;

import java.util.function.Function;

import static java.lang.Math.clamp;

@FunctionalInterface
public interface Interpolator extends Function<Float, Float> {

    //should get condition and return interpolated value on get
    // -> is boolean condition sensible for elementary state?
    //should be able to mix several channels (optionally based on weights)
    //should only use delta time -> requires length of transition in order to get linear factor
    //should map linear factor using a supplied transition function

    Interpolator LINEAR = t -> t;
    Interpolator QUADRATIC = t -> t * t;
    Interpolator INV_QUADRATIC = t -> 1 - ((t - 1) * (t - 1));
    Interpolator SMOOTHSTEP = t -> -2 * t * t * t + 3 * t * t;

    default Float interpolate(Float t) {
        return apply(clamp(t, 0, 1));
    }

}
