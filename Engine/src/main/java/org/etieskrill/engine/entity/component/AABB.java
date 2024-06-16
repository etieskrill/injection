package org.etieskrill.engine.entity.component;

import org.joml.Vector3f;
import org.joml.Vector3fc;

public class AABB {

    protected final Vector3f min;
    protected final Vector3f max;
    protected final Vector3f center;
    protected final Vector3f size;

    public AABB(Vector3fc min, Vector3fc max) {
        this(min, max,
                new Vector3f(max).sub(min).mul(.5f).add(min),
                new Vector3f(max).sub(min).absolute()
        );
    }

    public AABB(Vector3fc min, Vector3fc max, Vector3fc center, Vector3fc size) {
        if (min.x() > max.x() || min.y() > max.y() || min.z() > max.z()) {
            throw new IllegalArgumentException(
                    "Bounding box mininum corner components must be smaller than those of maximum corner");
        }

        this.min = (Vector3f) min;
        this.max = (Vector3f) max;
        this.center = (Vector3f) center;
        this.size = (Vector3f) size;
    }

    public Vector3fc getMin() {
        return min;
    }

    public Vector3fc getMax() {
        return max;
    }

    public Vector3fc getCenter() {
        return center;
    }

    public Vector3fc getSize() {
        return size;
    }

    public void set(Vector3fc min, Vector3fc max) {
        this.min.set(min);
        this.max.set(max);
        this.max.sub(this.min, this.center).mul(.5f).add(this.min);
        this.max.sub(this.min, this.size).absolute();
    }

    public boolean overlapsWith(AABB other) {
        return !(min.x() >= other.getMax().x()
                || min.y() >= other.getMax().y()
                || min.z() >= other.getMax().z()
                || max.x() <= other.getMin().x()
                || max.y() <= other.getMin().y()
                || max.z() <= other.getMin().z());
    }

    @Override
    public String toString() {
        return "AABB{" +
                "min=" + min +
                ", max=" + max +
                ", center=" + center +
                ", size=" + size +
                '}';
    }

}
