package org.etieskrill.engine.graphics.assimp;

import glm_.vec2.Vec2;
import glm_.vec3.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class Vertex {
    
    public static final int
            POSITION_COMPONENTS = 3,
            NORMAL_COMPONENTS = 3,
            TEXTURE_COMPONENTS = 2,
            COMPONENTS = 8;
    
    private final Vec3 position;
    private final Vec3 normal;
    private final Vec2 textureCoords;
    
    public Vertex(Vec3 position, Vec3 normal, Vec2 textureCoords) {
        this.position = position;
        this.normal = normal;
        this.textureCoords = textureCoords;
    }
    
    public Vec3 getPosition() {
        return position;
    }
    
    public Vec3 getNormal() {
        return normal;
    }
    
    public Vec2 getTextureCoords() {
        return textureCoords;
    }
    
    public List<Float> toList() { //TODO optimise this
        List<Float> data = new ArrayList<>();
        for (float[] arr : new float[][]{position.getArray(), normal.getArray(), textureCoords.getArray()})
            for (Float f : arr) data.add(f);
        return data;
    }
    
    @Override
    public String toString() {
        return "pos=" + position.toString() + ", norm=" + normal.toString() + ", tex=" + textureCoords.toString();
    }
    
}
