package org.etieskrill.engine.graphics.gl;

import glm.mat._4.Mat4;
import glm.vec._3.Vec3;

public class RawModel {

    public static final int
            MODEL_POSITION_COMPONENTS = 3,
            MODEL_NORMAL_COMPONENTS = 3,
            MODEL_TEXTURE_COMPONENTS = 2;
    
    public enum ModelComponents {
        MODEL_POSITION_COMPONENTS(3),
        MODEL_NORMAL_COMPONENTS(3),
        MODEL_TEXTURE_COMPONENTS(2);
    
        private final int components;
        
        ModelComponents(int numComponents) {
            this.components = numComponents;
        }
    
        public int getComponents() {
            return components;
        }
    }
    
    private final int vao;
    private final int numVertices;
    private final int drawMode;
    private final boolean indexBuffer;

    private final Vec3 position;
    private float scale;
    private float rotation;
    private final Vec3 rotationAxis;

    private final Mat4 transform;

    public RawModel(int vao, int numVertices, int drawMode) {
        this(vao, numVertices, drawMode, true);
    }

    public RawModel(int vao, int numVertices, int drawMode, boolean indexBuffer) {
        this(vao, numVertices, drawMode, indexBuffer, new Vec3(), 1f, 0f, new Vec3(), new Mat4());
    }
    
    public RawModel(int vao, int numVertices, int drawMode, boolean indexBuffer,
                    Vec3 position, float scale, float rotation, Vec3 rotationAxis, Mat4 transform) {
        this.vao = vao;
        this.numVertices = numVertices;
        this.drawMode = drawMode;
        this.indexBuffer = indexBuffer;
        this.position = position;
        this.scale = scale;
        this.rotation = rotation;
        this.rotationAxis = rotationAxis;
        this.transform = transform;
        updateTransform();
    }
    
    protected RawModel(RawModel rawModel) {
        this(rawModel.getVao(), rawModel.getNumVertices(), rawModel.getDrawMode(), rawModel.hasIndexBuffer(),
                rawModel.position, rawModel.scale, rawModel.rotation, rawModel.rotationAxis, rawModel.transform);
    }

    public Vec3 getPosition() {
        return position;
    }

    public RawModel setPosition(Vec3 newPosition) {
        this.position.set(newPosition);
        updateTransform();
        return this;
    }

    public float getScale() {
        return scale;
    }

    public RawModel setScale(float scale) {
        this.scale = scale;
        updateTransform();
        return this;
    }

    public float getRotation() {
        return rotation;
    }
    
    public Vec3 getRotationAxis() {
        return rotationAxis;
    }
    
    public RawModel setRotation(float rotation, Vec3 rotationAxis) {
        this.rotation = rotation;
        //thanks java glm documentation for specifying so clearly that this vector needs to be normalised
        this.rotationAxis.set(rotationAxis).normalize();
        updateTransform();
        return this;
    }

    private void updateTransform() {
        transform.set(transform.identity().translate(position).scale(scale).rotate(rotation, rotationAxis));
    }

    protected int getVao() {
        return vao;
    }
    
    public int getNumVertices() {
        return numVertices;
    }

    public int getDrawMode() {
        return drawMode;
    }

    public boolean hasIndexBuffer() {
        return indexBuffer;
    }

    public Mat4 getTransform() {
        return transform;
    }
    
    public void setTransform(Mat4 transform) {
        this.transform.set(transform);
    }

}
