package org.etieskrill.engine.graphics.assimp;

import org.etieskrill.engine.math.Vec2f;
import org.etieskrill.engine.math.Vec3f;

import java.util.List;
import java.util.stream.Stream;

public class Vertex {
    
    public static final int
            POSITION_COMPONENTS = 3,
            NORMAL_COMPONENTS = 3,
            TEXTURE_COMPONENTS = 2,
            COMPONENTS = 8;
    
    private final Vec3f position;
    private final Vec3f normal;
    private final Vec2f textureCoords;
    
    public Vertex(Vec3f position, Vec3f normal, Vec2f textureCoords) {
        this.position = position;
        this.normal = normal;
        this.textureCoords = textureCoords;
    }
    
    public Vec3f getPosition() {
        return position;
    }
    
    public Vec3f getNormal() {
        return normal;
    }
    
    public Vec2f getTextureCoords() {
        return textureCoords;
    }
    
    public List<Float> toList() {
        return Stream.of(position.toList(), normal.toList(), textureCoords.toList())
                .flatMap(List::stream)
                .toList();
    }
    
    @Override
    public String toString() {
        return "pos=" + position.toString() + ", norm=" + normal.toString() + ", tex=" + textureCoords.toString();
    }
    
}
