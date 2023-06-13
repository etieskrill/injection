package org.etieskrill.engine.math;

public class Vec4f {
        
    private float x, y, z, w;
    
    public Vec4f(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }
    
    public Vec4f(Vec4f vec) {
        this(vec.x, vec.y, vec.z, vec.w);
    }
    
    public Vec4f(float f) {
        this(f, f, f, f);
    }
    
    public Vec4f() {
        this(0f, 0f, 0f, 0f);
    }
    
    public Vec4f add(Vec4f vec) {
        return new Vec4f(x + vec.x, y + vec.y, z + vec.z, w + vec.w);
    }
    
    public Vec4f sub(Vec4f vec) {
        return new Vec4f(x - vec.x, y - vec.y, z - vec.z, w - vec.w);
    }
    
    public Vec4f scl(float s) {
        return new Vec4f(x * s, y * s, z * s, w * s);
    }
    
    public float mag2() {
        return x * x + y * y + z * z + w * w;
    }
    
    public float mag() {
        return (float) Math.sqrt(mag2());
    }
    
    public Vec4f normalise() {
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
    
    public float getW() {
        return w;
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
    
    public void setW(float w) {
        this.w = w;
    }
    
    public void set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }
    
    public void set(Vec4f vec) {
        set(vec.x, vec.y, vec.z, vec.w);
    }
    
    public void setZero() {
        x = 0f;
        y = 0f;
        z = 0f;
        w = 0f;
    }
    
    @Override
    public String toString() {
            return String.format("[%.3f, %.3f, %.3f, %.3f]", getX(), getY(), getZ(), getW());
        }
    
}
