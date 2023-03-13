package org.etieskrill.injection.particle.generation;

import java.util.Random;

public class RandomSizeStrategy implements SizeStrategy {

    private final float minSize;
    private final float maxSize;
    private final Random random;

    public RandomSizeStrategy(float minSize, float maxSize) {
        if (minSize >= maxSize) throw new IllegalArgumentException("Maximum size must be larger than minimum size");
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.random = new Random();
    }

    @Override
    public Float get() {
        return random.nextFloat(minSize, maxSize);
    }

}
