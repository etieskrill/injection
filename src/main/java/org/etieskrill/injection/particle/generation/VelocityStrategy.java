package org.etieskrill.injection.particle.generation;

import org.etieskrill.injection.math.Interpolator;
import org.etieskrill.injection.math.Vector2;

import java.util.function.Supplier;

public interface VelocityStrategy extends Supplier<Vector2> {}
