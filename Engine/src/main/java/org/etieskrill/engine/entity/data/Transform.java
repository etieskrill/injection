package org.etieskrill.engine.entity.data;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Transform {

    private final Vector3f position;
    private float rotation;
    private final Vector3f rotationAxis;
    private final Vector3f scale;

    private final Vector3f initialPosition;
    private float initialRotation;
    private final Vector3f initialRotationAxis;
    private final Vector3f initialScale;

    private final Matrix4f transform;
    
    private boolean dirty = true;
    
    public static Transform getBlank() {
        return new Transform(new Vector3f(), 0, new Vector3f(1, 0, 0), new Vector3f(1));
    }

    public Transform(Vector3f position, float rotation, Vector3f rotationAxis, Vector3f scale) {
        this.position = position;
        this.rotation = rotation;
        this.rotationAxis = rotationAxis;
        this.scale = scale;

        this.initialPosition = new Vector3f(0);
        this.initialRotation = 0;
        this.initialRotationAxis = new Vector3f(1, 0, 0);
        this.initialScale = new Vector3f(1);

        this.transform = new Matrix4f().identity();
    }
    
    public Transform(Transform transform) {
        this(new Vector3f(transform.position), transform.rotation, new Vector3f(transform.rotationAxis), new Vector3f(transform.scale));
        setInitialPosition(transform.initialPosition);
        setInitialRotation(transform.initialRotation, new Vector3f(transform.initialRotationAxis));
        setInitialScale(transform.initialScale);
        this.dirty = transform.dirty;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Transform setPosition(Vector3f vec) {
        this.position.set(vec);
        dirty();
        return this;
    }
    
    public Transform setPosition(Transform transform) {
        return setPosition(transform.getPosition());
    }

    public Transform translate(Vector3f vec) {
        this.position.add(vec);
        dirty();
        return this;
    }
    
    public Transform translate(Transform transform) {
        return translate(transform.getPosition());
    }

    public Vector3f getScale() {
        return scale;
    }

    public Transform setScale(Vector3f scale) {
        if (scale.x() < 0 || scale.y() < 0 || scale.z() < 0)
            throw new IllegalArgumentException("Cannot apply negative scaling factor");
        this.scale.set(scale);
        dirty();
        return this;
    }
    
    public Transform setScale(float scale) {
        return setScale(new Vector3f(scale));
    }
    
    public Transform setScale(Transform transform) {
        return setScale(transform.getScale());
    }
    
    public float getRotation() {
        return rotation;
    }

    public Vector3f getRotationAxis() {
        return rotationAxis;
    }

    public Transform setRotation(float rotation, Vector3f rotationAxis) {
        this.rotation = rotation;
        this.rotationAxis.set(rotationAxis.normalize());
        dirty();
        return this;
    }
    
    public Transform setRotation(Transform transform) {
        return setRotation(transform.getRotation(), transform.getRotationAxis());
    }
    
    //Transform is lazily updated
    public Matrix4f toMat() {
        if (isDirty()) updateTransform();
        return transform;
    }
    
    public Transform set(Transform transform) {
        setPosition(transform);
        setRotation(transform);
        setScale(transform);
        return this;
    }

    public Transform setInitialPosition(Vector3f position) {
        this.initialPosition.set(position);
        dirty();
        return this;
    }

    public Transform setInitialRotation(float rotation, Vector3f rotationAxis) {
        this.initialRotation = rotation;
        this.initialRotationAxis.set(rotationAxis);
        dirty();
        return this;
    }

    public Transform setInitialScale(Vector3f scale) {
        this.initialScale.set(scale);
        dirty();
        return this;
    }
    
    private void updateTransform() {
        this.transform.identity()
                .translate(initialPosition.add(position))
                .rotate(initialRotation, initialRotationAxis)
                .rotate(rotation, rotationAxis)
                .scale(scale.mul(initialScale));
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

    public Vector3f getInitialPosition() {
        return initialPosition;
    }
    
}
