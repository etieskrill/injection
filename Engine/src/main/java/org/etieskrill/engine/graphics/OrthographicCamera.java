package org.etieskrill.engine.graphics;

import glm.mat._4.Mat4;
import glm.vec._3.Vec3;

public class OrthographicCamera extends Camera {
    
    //private float top = -540f, bottom = 540f, left = -960f, right = 960f;
    private float top = -1080f, bottom = 0f, left = 0f, right = 1920f;
    
    public OrthographicCamera() {
        super();
        setPerspective(new Mat4().ortho(left, right, bottom, top, near, far));
    }

    @Override
    protected void updatePerspective() {
    }

    public OrthographicCamera setTop(float top) {
        this.top = top;
        return this;
    }
    
    public OrthographicCamera setBottom(float bottom) {
        this.bottom = bottom;
        return this;
    }
    
    public OrthographicCamera setLeft(float left) {
        this.left = left;
        return this;
    }
    
    public OrthographicCamera setRight(float right) {
        this.right = right;
        return this;
    }
    
}
