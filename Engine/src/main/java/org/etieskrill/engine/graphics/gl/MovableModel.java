package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.math.Vec3f;

public final class MovableModel extends RawModel {
    
    private final Vec3f position;
    //private float rotation;
    
    //private final float[] vertices;
    //private final short[] indices;
    
    private boolean moved;
    
    public MovableModel(RawModel rawModel) {
        super(rawModel);
        this.position = new Vec3f();
    }
    
    @Override
    public void bind() {
    
    }
    
    @Override
    public void unbind() {
    
    }
    
    public Vec3f getPosition() {
        return position;
    }
    
    public synchronized void updatePosition(Vec3f newPosition) {
        this.position.set(newPosition);
        
        float[] vertices = getVertices();
        float[] positionVertices = new float[vertices.length];
        for (int i = 0; i < vertices.length; i += MODEL_POSITION_COMPONENTS) { //TODO resolve dimensionality
            positionVertices[i] = vertices[i] + position.getX();
            positionVertices[i + 1] = vertices[i + 1] + position.getY();
            positionVertices[i + 2] = vertices[i + 2] + position.getZ();
        }
    
        super.update(positionVertices, getColours(), getTextures(), getIndices(), getDrawMode());
    }
    
}
