package org.etieskrill.engine.entity.component;

import org.joml.Vector3f;

public class Acceleration extends DirectionalForceComponent {

    private float factor;

    public Acceleration(Vector3f force) {
        this(force, 1);
    }

    public Acceleration(Vector3f force, float factor) {
        super(force);
        this.factor = factor;
    }

    public float getFactor() {
        return factor;
    }

    public void setFactor(float factor) {
        this.factor = factor;
    }

}
