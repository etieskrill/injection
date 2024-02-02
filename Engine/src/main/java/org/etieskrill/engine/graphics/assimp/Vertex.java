package org.etieskrill.engine.graphics.assimp;

import org.joml.Vector2fc;
import org.joml.Vector3fc;

import java.util.List;

public class Vertex {
    
    public static final int
            POSITION_COMPONENTS = 3,
            NORMAL_COMPONENTS = 3,
            TEXTURE_COMPONENTS = 2,
            COMPONENTS = 8;

    private final Vector3fc position;
    private final Vector3fc normal;
    private final Vector2fc textureCoords;

    public Vertex(Vector3fc position, Vector3fc normal, Vector2fc textureCoords) {
        this.position = position;
        this.normal = normal;
        this.textureCoords = textureCoords;
    }

    public Vector3fc getPosition() {
        return position;
    }

    public Vector3fc getNormal() {
        return normal;
    }

    public Vector2fc getTextureCoords() {
        return textureCoords;
    }
    
    public List<Float> toList() { //TODO optimise this
        return List.of(
                position.x(), position.y(), position.z(),
                normal.x(), normal.y(), normal.z(),
                textureCoords.x(), textureCoords.y()
        );
    }
    
    @Override
    public String toString() {
        return "pos=" + position.toString() + ", norm=" + normal.toString() + ", tex=" + textureCoords.toString();
    }
    
}
