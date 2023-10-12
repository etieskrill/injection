package org.etieskrill.engine.entity.data;

import glm_.vec3.Vec3;

public class AABB {
    
    private final Vec3 min;
    private final Vec3 max;
    
    public AABB(Vec3 min, Vec3 max) {
        this.min = min;
        this.max = max;
    }
    
    public Vec3 getMin() {
        return min;
    }
    
    public Vec3 getMax() {
        return max;
    }
    
    public Vec3 getCenter() {
        return max.minus(min).times(0.5).plus(min);
    }
    
    public Vec3 getSize() {
        return max.minus(min);
    }
    
    @Override
    public String toString() {
        return "AABB{" +
                "min=" + min +
                ", max=" + max +
                '}';
    }
    
}
