package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.math.Vec2f;

import java.util.Arrays;

public class MovableModel extends RawModel {
    
    private final Vec2f position;
    //private float rotation;
    
    public MovableModel(RawModel rawModel) {
        super(rawModel);
        this.position = new Vec2f();
    }
    
    public Vec2f getPosition() {
        return position;
    }
    
    public void updatePosition(Vec2f newPosition) {
        this.position.set(newPosition);
        
        float[] vertices = getVertices();
        float[] positionVertices = new float[vertices.length];
    
        for (int i = 0; i < vertices.length; i+=2) { //TODO resolve dimensionality
            positionVertices[i] = vertices[i] + position.getX();
            positionVertices[i + 1] = vertices[i + 1] + position.getY();
        }
    
        super.update(positionVertices, getIndices(), getDrawMode());
    }
    
}
