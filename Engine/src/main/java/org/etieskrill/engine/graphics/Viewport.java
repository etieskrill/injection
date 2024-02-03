package org.etieskrill.engine.graphics;


import org.joml.Vector2f;

//TODO fixed viewport for now
//technically, this does not matter much for now
public class Viewport {

    private final Vector2f size;

    private Camera camera;

    public Viewport(Vector2f size, Camera camera) {
        this.size = size;
        this.camera = camera;
    }



}
