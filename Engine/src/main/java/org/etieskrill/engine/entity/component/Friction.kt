package org.etieskrill.engine.entity.component;

public class Friction {

    private float coefficient;

    public Friction(float coefficient) {
        this.coefficient = coefficient;
    }

    public float getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(float coefficient) {
        this.coefficient = coefficient;
    }

}
