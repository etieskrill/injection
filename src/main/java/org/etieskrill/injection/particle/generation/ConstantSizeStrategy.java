package org.etieskrill.injection.particle.generation;

public class ConstantSizeStrategy implements SizeStrategy {

    private final float size;

    public ConstantSizeStrategy(float size) {
        this.size = size;
    }

    @Override
    public Float get() {
        return size;
    }

}
