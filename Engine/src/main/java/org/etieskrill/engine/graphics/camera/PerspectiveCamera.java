package org.etieskrill.engine.graphics.camera;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector2ic;

public class PerspectiveCamera extends Camera {

    private float fov;

    public PerspectiveCamera(Vector2ic viewportSize) {
        super(viewportSize);
        setFar(100f); //TODO figure out why this is negative in ortho
        setZoom(3.81f); //setFov(60f);
        setPerspective(new Matrix4f().perspective((float) Math.toRadians(fov),
                (float) viewportSize.x() / viewportSize.y(), near, far));

        if (autoUpdate) update();
    }

    @Override
    protected void updatePerspective() {
        this.fov = (((110f - 30f) / (10f - 0.1f)) * (zoom - 0.1f) + 30f);
        this.perspective.setPerspective((float) Math.toRadians(fov),
                (float) viewportSize.x() / viewportSize.y(), near, far);
    }

    public float getFov() {
        return fov;
    }
    
    //TODO make fov and the whole zoom mechanic actually usable
    public void setFov(float fov) {
        this.fov = fov;
    }
    
}
