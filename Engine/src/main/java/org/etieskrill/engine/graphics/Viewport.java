package org.etieskrill.engine.graphics;


import glm_.vec2.Vec2;

//TODO fixed viewport for now
//technically, this does not matter much for now
public class Viewport {

    private final Vec2 size;

    private Camera camera;

    public Viewport(Vec2 size, Camera camera) {
        this.size = size;
        this.camera = camera;
    }



}
