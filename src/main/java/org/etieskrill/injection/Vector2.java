package org.etieskrill.injection;

public class Vector2 {

    private float x;
    private float y;

    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2() {
        this.x = 0f;
        this.y = 0f;
    }

    public Vector2(Vector2 vec) {
        this.x = vec.getX();
        this.y = vec.getY();
    }

    public float length2() {
        return getX() * getX() + getY() * getY();
    }

    public float length() {
        return (float) Math.sqrt(length2());
    }

    public Vector2 add(Vector2 vec) {
        return new Vector2(getX() + vec.getX(), getY() + vec.getY());
    }

    public Vector2 sub(Vector2 vec) {
        return new Vector2(getX() - vec.getX(), getY() - vec.getY());
    }

    public Vector2 scl(float s) {
        return new Vector2(getX() * s, getY() * s);
    }

    public float dot(Vector2 vec) {
        return getX() * vec.getX() + getY() * vec.getY();
    }

    public Vector2 normal() {
        return scl(1 / length());
    }

    public Vector2 set(Vector2 vec) {
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
