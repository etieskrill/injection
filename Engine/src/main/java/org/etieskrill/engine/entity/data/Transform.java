package org.etieskrill.engine.entity.data;

import glm_.mat4x4.Mat4;
import glm_.vec3.Vec3;

public class Transform {
    
    private final Vec3 position;
    private float rotation;
    private final Vec3 rotationAxis;
    private final Vec3 scale;
    
    private final Vec3 initialPosition;
    private float initialRotation;
    private final Vec3 initialRotationAxis;
    private final Vec3 initialScale;
    
    private final Mat4 transform;
    
    private boolean dirty = true;
    
    public static Transform getBlank() {
        return new Transform(new Vec3(), 0, new Vec3(1, 0, 0), new Vec3(1));
    }
    
    public Transform(Vec3 position, float rotation, Vec3 rotationAxis, Vec3 scale) {
        this.position = position;
        this.rotation = rotation;
        this.rotationAxis = rotationAxis;
        this.scale = scale;
        
        this.initialPosition = new Vec3(0);
        this.initialRotation = 0;
        this.initialRotationAxis = new Vec3(1, 0, 0);
        this.initialScale = new Vec3(1);
        
        this.transform = new Mat4(1);
    }
    
    public Transform(Transform transform) {
        this(new Vec3(transform.position), transform.rotation, new Vec3(transform.rotationAxis), new Vec3(transform.scale));
        setInitialPosition(transform.initialPosition);
        setInitialRotation(transform.initialRotation, new Vec3(transform.initialRotationAxis));
        setInitialScale(transform.initialScale);
        this.dirty = transform.dirty;
    }
    
    public Vec3 getPosition() {
        return position;
    }
    
    public Transform setPosition(Vec3 vec) {
        this.position.put(vec);
        dirty();
        return this;
    }
    
    public Transform setPosition(Transform transform) {
        return setPosition(transform.getPosition());
    }
    
    public Transform translate(Vec3 vec) {
        this.position.plusAssign(vec);
        dirty();
        return this;
    }
    
    public Transform translate(Transform transform) {
        return translate(transform.getPosition());
    }
    
    public Vec3 getScale() {
        return scale;
    }
    
    public Transform setScale(Vec3 scale) {
        if (scale.anyLessThan(0))
            throw new IllegalArgumentException("Cannot apply negative scaling factor");
        this.scale.put(scale);
        dirty();
        return this;
    }
    
    public Transform setScale(float scale) {
        return setScale(new Vec3(scale));
    }
    
    public Transform setScale(Transform transform) {
        return setScale(transform.getScale());
    }
    
    public float getRotation() {
        return rotation;
    }
    
    public Vec3 getRotationAxis() {
        return rotationAxis;
    }
    
    public Transform setRotation(float rotation, Vec3 rotationAxis) {
        this.rotation = rotation;
        this.rotationAxis.put(rotationAxis.normalize());
        dirty();
        return this;
    }
    
    public Transform setRotation(Transform transform) {
        return setRotation(transform.getRotation(), transform.getRotationAxis());
    }
    
    //Transform is lazily updated
    public Mat4 toMat() {
        if (isDirty()) updateTransform();
        return transform;
    }
    
    public Transform set(Transform transform) {
        setPosition(transform);
        setRotation(transform);
        setScale(transform);
        return this;
    }
    
    public Transform setInitialPosition(Vec3 position) {
        this.initialPosition.put(position);
        dirty();
        return this;
    }
    
    public Transform setInitialRotation(float rotation, Vec3 rotationAxis) {
        this.initialRotation = rotation;
        this.initialRotationAxis.put(rotationAxis);
        dirty();
        return this;
    }
    
    public Transform setInitialScale(Vec3 scale) {
        this.initialScale.put(scale);
        dirty();
        return this;
    }
    
    private void updateTransform() {
        this.transform.invoke(1)
                .rotateAssign(initialRotation, initialRotationAxis)
                .translateAssign(initialPosition)
                .rotateAssign(rotation, rotationAxis)
                .translateAssign(position)
                .scale(scale.times(initialScale));
    }
    
    private void dirty() {
        dirty = true;
    }
    
    private boolean isDirty() {
        if (!dirty) return false;
        dirty = false;
        return true;
    }
    
    @Override
    public String toString() {
        return "Transform{" +
                "position=" + position +
                ", rotation=" + rotation +
                ", rotationAxis=" + rotationAxis +
                ", scale=" + scale +
                '}';
    }
    
    public Vec3 getInitialPosition() {
        return initialPosition;
    }
    
}
