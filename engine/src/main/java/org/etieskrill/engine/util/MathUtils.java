package org.etieskrill.engine.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class MathUtils {

    private MathUtils() {
    }

    @Contract(mutates = "param1")
    public static List<Float> normalise(@NotNull List<@NotNull Float> components) {
        if (components.stream().anyMatch(component -> component < 0f))
            throw new IllegalArgumentException("Components must not be negative");
        float componentSum = (float) components.stream().mapToDouble(Number::doubleValue).sum();
        if (componentSum == 0) {
            Collections.fill(components, (float) 0);
            return components;
        }
        if (Math.abs(1 - componentSum) > 0.0001) {
            components.replaceAll(n ->
                    n * (1f / componentSum)
            );
        }
        return components;
    }

    @Contract(mutates = "param1")
    public static List<Double> normaliseD(@NotNull List<@NotNull Double> components) {
        double componentSum = components.stream().mapToDouble(Number::doubleValue).sum();
        if (componentSum == 0) {
            Collections.fill(components, 0d);
            return components;
        }
        if (Math.abs(1 - componentSum) > 0.0001) {
            components.replaceAll(n ->
                    n * (1f / componentSum)
            );
        }
        return components;
    }

}
