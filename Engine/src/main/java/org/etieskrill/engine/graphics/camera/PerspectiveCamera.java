package org.etieskrill.engine.graphics.camera;

import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4f;
import org.joml.Vector2ic;

@Getter
@Setter
public class PerspectiveCamera extends Camera {

    /**
     * The (vertical) field of view in degrees.
     */
    private float fov;

    public PerspectiveCamera(Vector2ic viewportSize) {
        super(viewportSize);
        setFar(100f); //TODO figure out why this is negative in ortho
        setZoom(3.81f); //setFov(60f);
        setPerspective(new Matrix4f().perspective((float) Math.toRadians(fov),
                (float) viewportSize.x() / viewportSize.y(), near, far));

        dirty();
        update();
    }

    @Override
    protected void updatePerspective() {
        //TODO make fov and the whole zoom mechanic actually usable, cuz what in tarnation is this shit
        this.fov = (((110f - 30f) / (10f - 0.1f)) * (zoom - 0.1f) + 30f);
        this.perspective.setPerspective((float) Math.toRadians(fov),
                (float) viewportSize.x() / viewportSize.y(), near, far);
    }

}
