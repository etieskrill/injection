package org.etieskrill.engine.math;

import glm.mat._4.Mat4;

import java.util.Arrays;

public class Mat4f {
    
    private static final float[] identity = {
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
    };
    
    private final float[] values;
    
    public Mat4f() {
        this(1f);
    }
    
    public Mat4f(float f) {
        this(
                f, 0f, 0f, 0f,
                0f, f, 0f, 0f,
                0f, 0f, f, 0f,
                0f, 0f, 0f, f
        );
    }
    
    public Mat4f(float a11, float a12, float a13, float a14,
                  float a21, float a22, float a23, float a24,
                  float a31, float a32, float a33, float a34,
                  float a41, float a42, float a43, float a44) {
        values = new float[]{
                a11, a12, a13, a14,
                a21, a22, a23, a24,
                a31, a32, a33, a34,
                a41, a42, a43, a44
        };
    }
    
    public Mat4f(float[] values) {
        if (values.length != 16)
            throw new IllegalArgumentException("4x4 Matrix requires exactly 16 values");
        
        this.values = values;
    }
    
    public static Mat4f identity() {
        return new Mat4f(Arrays.copyOf(identity, identity.length));
    }
    
    public static Mat4f zero() {
        return new Mat4f(new float[16]);
    }
    
    public Mat4f scale(float scalar) {
        float[] arr = Arrays.copyOf(values, values.length);
        
        for (int i = 0; i < arr.length; i++) {
            arr[i] = arr[i] * scalar;
        }
        
        return new Mat4f(arr);
    }
    
    public Mat4f add(Mat4f mat) {
        float[] arr = Arrays.copyOf(values, values.length);
    
        for (int i = 0; i < arr.length; i++) {
            arr[i] = arr[i] + mat.values[i];
        }
    
        return new Mat4f(arr);
    }
    
    /*public Mat4f mult(Mat4f mat) {
        float[] val = mat.values;
        
        return new Mat4f(
                values[0] * val[0]
        );
    }
    
    public static Mat4f rotate() {
    }*/
    
    public static Mat4f translate(Vec3f vec) {
        return identity().add(new Mat4f(
                0f, 0f, 0f, vec.getX(),
                0f, 0f, 0f, vec.getY(),
                0f, 0f, 0f, vec.getZ(),
                0f, 0f, 0f, 0f
        ));
    }
    
    public float[] getValues() {
        return values;
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(128);
        for (int i = 0; i < 16; i += 4) {
            buffer.append(String.format("[%.3f, %.3f, %.3f, %.3f]\n",
                    values[i], values[i + 1], values[i + 2], values[i + 3]));
        }
        return buffer.toString();
    }
    
}
