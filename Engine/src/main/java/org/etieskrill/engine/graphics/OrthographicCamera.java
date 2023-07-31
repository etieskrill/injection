package org.etieskrill.engine.graphics;

import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import org.etieskrill.engine.math.Vec2f;

public class OrthographicCamera extends Camera {
    
    private final Vec2f size;
    private float top, bottom, left, right;
    
    public OrthographicCamera(Vec2f size, Vec3 origin) {
        super();
        this.size = new Vec2f();
        setSize(size);
        setPosition(origin);
        setPerspective(new Mat4().ortho(left, right, bottom, top, near, far));

        if (autoUpdate) update();
    }

    public OrthographicCamera(Vec2f size) {
        this(size, new Vec3(0f));
    }

    @Override
    protected void updatePerspective() {
        //TODO proper zoom
        float zoom = 1f / this.zoom;
        perspective.set(new Mat4().ortho(zoom * left, zoom * right, zoom * bottom, zoom * top, zoom * near, zoom * far));
    }

    @Override
    public Camera setPosition(Vec3 position) {
        this.position.set(position);
        updateDimensions();
        if (autoUpdate) update();
        return this;
    }

    public void setSize(Vec2f size) {
        this.size.set(size);
        updateDimensions();
        if (autoUpdate) update();
    }

    private void updateDimensions() {
        bottom = position.y;
        top = -size.getY() + bottom; //negative because ... reasons
        left = position.x;
        right = size.getX() + left;
    }
    
}
