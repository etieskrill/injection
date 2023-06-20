package org.etieskrill.engine.graphics;

import glm.mat._4.Mat4;

public class OrthographicCamera extends Camera {
    
    private float top = 1f, bottom = -1f, left = -1f, right = 1f;
    
    public OrthographicCamera() {
        super();
        setPerspective(new Mat4().ortho(left, right, bottom, top, near, far));
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
