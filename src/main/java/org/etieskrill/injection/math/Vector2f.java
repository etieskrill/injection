package org.etieskrill.injection.math;

public class Vector2f {

    private float x;
    private float y;

    public Vector2f(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2f() {
        this.x = 0f;
        this.y = 0f;
    }

    public Vector2f(Vector2f vec) {
        this(vec.getX(), vec.getY());
    }

    public float length2() {
        return getX() * getX() + getY() * getY();
    }

    public float length() {
        return (float) Math.sqrt(length2());
    }

    public Vector2f add(Vector2f vec) {
        return new Vector2f(getX() + vec.getX(), getY() + vec.getY());
    }

    public Vector2f sub(Vector2f vec) {
        return new Vector2f(getX() - vec.getX(), getY() - vec.getY());
    }

    public Vector2f scl(float s) {
        return new Vector2f(getX() * s, getY() * s);
    }

    public float dot(Vector2f vec) {
        return getX() * vec.getX() + getY() * vec.getY();
    }

    public Vector2f normal() {
        return scl(1 / length());
    }

    public Vector2f interpolate(Vector2f vec, float f) {
        if (f < 0f || f > 1f) throw new IllegalArgumentException("Supplied value must be normalised");
        return new Vector2f(getX() + (vec.getX() - getX()) * f, getY() + (vec.getY() - getY()) * f);
    }

    public Vector2f set(Vector2f vec) {
        this.x = vec.getX();
        this.y = vec.getY();
        return this;
    }

    public void setZero() {
        this.x = 0;
        this.y = 0;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "(" + this.x + "," + this.y + ")";
    }

}
