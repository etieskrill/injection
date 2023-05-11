package org.etieskrill.engine.graphics.gl;

import glm.mat._4.Mat4;
import glm.vec._3.Vec3;

import java.util.Arrays;

public class RawModel {

    public static final int
            MODEL_POSITION_COMPONENTS = 3,
            MODEL_COLOUR_COMPONENTS = 4,
            MODEL_TEXTURE_COMPONENTS = 2;
    
    public enum ModelComponents {
        MODEL_POSITION_COMPONENTS(3),
        MODEL_COLOUR_COMPONENTS(4),
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

    private Vec3 position = new Vec3();
    private float scale = 1f;
    private float rotation = 0f;
    private Vec3 rotationAxis = new Vec3();

    private Mat4 transform = new Mat4();

    public RawModel(int vao, int numVertices, int drawMode) {
        this.vao = vao;
        this.numVertices = numVertices;
        this.drawMode = drawMode;
        this.indexBuffer = true;
    }

    public RawModel(int vao, int numVertices, int drawMode, boolean indexBuffer) {
        this.vao = vao;
        this.numVertices = numVertices;
        this.drawMode = drawMode;
        this.indexBuffer = indexBuffer;
    }
    
    public RawModel(RawModel rawModel) {
        this(rawModel.getVao(), rawModel.getNumVertices(), rawModel.getDrawMode(), rawModel.hasIndexBuffer());
    }

    /*public static RawModel get(int vao, int vbo, int ebo) {
        return new RawModelWrapper(vao, vbo, ebo);
    }
    
    private static class RawModelWrapper extends RawModel {
        public RawModelWrapper(int vao, int vbo, int ebo) {
            super(vao, vbo, ebo);
        }
    
        @Override
        public void bind() {}
    
        @Override
        public void unbind() {}
    }*/
    
    public void bind() {};
    public void unbind() {};
    
    //public void update() {};

    public RawModel setPosition(Vec3 newPosition) {
        this.position.set(newPosition);
        updateTransform();
        return this;
    }

    public RawModel setScale(float scale) {
        this.scale = scale;
        updateTransform();
        return this;
    }

    public RawModel setRotation(float rotation, Vec3 rotationAxis) {
        this.rotation = rotation;
        this.rotationAxis = rotationAxis;
        updateTransform();
        return this;
    }

    private void updateTransform() {
        transform.set(transform.identity().translate(position).scale(scale).rotate(rotation, rotationAxis));
    }

    public void setTransform(Mat4 transform) {
        this.transform = transform;
    }

    public int getVao() {
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

}
