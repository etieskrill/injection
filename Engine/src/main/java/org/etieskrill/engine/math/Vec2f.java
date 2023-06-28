package org.etieskrill.engine.math;

public class Vec2f {
    
    private float x;
    private float y;
    
    public Vec2f(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public Vec2f(Vec2f vec) {
        this(vec.x, vec.y);
    }
    
    public Vec2f(float f) {
        this(f, f);
    }
    
    public Vec2f() {
        this(0f);
    }
    
    public Vec2f add(Vec2f another) {
        return new Vec2f(x + another.x, y + another.y);
    }
    
    public Vec2f sub(Vec2f another) {
        return new Vec2f(x - another.x, y - another.y);
    }
    
    public Vec2f scl(float s) {
        return new Vec2f(x * s, y * s);
    }
    
    public float mag2() {
        return x * x + y * y;
    }
    
    public float mag() {
        return (float) Math.sqrt(mag2());
    }
    
    public float length() {
        return mag();
    }
    
    public Vec2f normalise() {
        return scl(1 / mag());
    }
    
    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }
    
    public void setX(float x) {
        this.x = x;
    }
    
    public void setY(float y) {
        this.y = y;
    }
    
    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public void set(Vec2f vec) {
        set(vec.x, vec.y);
    }
    
    public void setZero() {
        x = 0f;
        y = 0f;
    }
    
    @Override
    public String toString() {
        return String.format("[%.3f, %.3f]", getX(), getY());
    }
    
}
