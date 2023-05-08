package org.etieskrill.engine.graphics.gl;

import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import org.etieskrill.engine.math.Vec3f;
import org.etieskrill.engine.util.FloatArrayMerger;
import org.lwjgl.opengl.GL33C;

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

    private Vec3 position = new Vec3();
    private float scale = 0f;
    private float rotation = 0f;
    private Vec3 rotationAxis = new Vec3();

    private Mat4 transform = new Mat4();

    public RawModel(int vao, int numVertices, int drawMode) {
        this.vao = vao;
        this.numVertices = numVertices;
        this.drawMode = drawMode;
    }
    
    public RawModel(RawModel rawModel) {
        this(rawModel.getVao(), rawModel.getNumVertices(), rawModel.getDrawMode());
    }

    protected RawModel() {
        this.vao = 0;
        this.numVertices = 0;
        this.drawMode = 0;
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

    public void setPosition(Vec3 newPosition) {
        this.position.set(newPosition);
        updateTransform();
    }

    public void setScale(float scale) {
        this.scale = scale;
        updateTransform();
    }

    public void setRotation(float rotation, Vec3 rotationAxis) {
        this.rotation = rotation;
        this.rotationAxis = rotationAxis;
        updateTransform();
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
    
}
