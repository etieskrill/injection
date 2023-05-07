package org.etieskrill.engine.math;

public class Vec3f {
    
    private float x, y, z;
    
    public Vec3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public Vec3f(Vec3f vec) {
        this(vec.x, vec.y, vec.z);
    }
    
    public Vec3f() {
        this(0f, 0f, 0f);
    }
    
    public Vec3f add(Vec3f another) {
        return new Vec3f(x + another.x, y + another.y, z + another.z);
    }
    
    public Vec3f sub(Vec3f another) {
        return new Vec3f(x - another.x, y - another.y, z - another.z);
    }
    
    public Vec3f scl(float s) {
        return new Vec3f(x * s, y * s, z * s);
    }
    
    public float mag2() {
        return x * x + y * y + z * z;
    }
    
    public float mag() {
        return (float) Math.sqrt(mag2());
    }
    
    public Vec3f normalise() {
        return scl(1 / mag());
    }
    
    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }
    
    public float getZ() {
        return z;
    }
    
    public void setX(float x) {
        this.x = x;
    }
    
    public void setY(float y) {
        this.y = y;
    }
    
    public void setZ(float z) {
        this.z = z;
    }
    
    public void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public void set(Vec3f vec) {
        set(vec.x, vec.y, vec.z);
    }
    
    public void setZero() {
        x = 0f;
        y = 0f;
        z = 0f;
    }
    
    @Override
    public String toString() {
        return String.format("[%.3f, %.3f, %.3f]", getX(), getY(), getZ());
    }
    
}
