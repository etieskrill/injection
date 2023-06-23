package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.math.Vec2f;

//TODO fixed viewport for now
public class Viewport {

    private final Vec2f size;

    private Camera camera;

    public Viewport(Vec2f size, Camera camera) {
        this.size = size;
        this.camera = camera;
    }



}
