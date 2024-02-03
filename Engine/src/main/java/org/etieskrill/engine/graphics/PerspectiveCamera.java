package org.etieskrill.engine.graphics;

import org.joml.Matrix4f;
import org.joml.Vector2f;

public class PerspectiveCamera extends Camera {

    private final Vector2f size;
    private float fov;

    public PerspectiveCamera(Vector2f size) {
        super();
        this.size = new Vector2f(size);
        setFar(100f); //TODO figure out why this is negative in ortho
        setZoom(3.81f); //setFov(60f);
        setPerspective(new Matrix4f().perspective((float) Math.toRadians(fov), size.x() / size.y(), near, far));

        if (autoUpdate) update();
    }

    @Override
    protected void updatePerspective() {
        this.fov = (((110f - 30f) / (10f - 0.1f)) * (zoom - 0.1f) + 30f);
        this.perspective.setPerspective((float) Math.toRadians(fov), size.x() / size.y(), near, far);
    }

    public Vector2f getSize() {
        return size;
    }

    public void setSize(Vector2f size) {
        this.size.set(size);
    }

    public float getFov() {
        return fov;
    }
    
    //TODO make fov and the whole zoom mechanic actually usable
    public void setFov(float fov) {
        this.fov = fov;
    }
    
}
