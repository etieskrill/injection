package org.etieskrill.engine.entity.data;

import org.jetbrains.annotations.Contract;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class AABB {

    private final Vector3fc min;
    private final Vector3fc max;

    public AABB(Vector3fc min, Vector3fc max) {
        this.min = min;
        this.max = max;
    }

    public Vector3fc getMin() {
        return min;
    }

    public Vector3fc getMax() {
        return max;
    }

    @Contract("-> new")
    public Vector3f getCenter() {
        return max.sub(min, new Vector3f()).mul(0.5f).add(min);
    }

    @Contract("-> new")
    public Vector3f getSize() {
        return max.sub(min, new Vector3f());
    }
    
    @Override
    public String toString() {
        return "AABB{" +
                "min=" + min +
                ", max=" + max +
                '}';
    }
    
}
