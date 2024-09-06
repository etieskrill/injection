package org.etieskrill.engine.graphics.camera;

import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4f;
import org.joml.Vector2ic;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import static java.lang.Math.abs;
import static java.lang.Math.max;

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

    @Override
    public boolean frustumTestSphere(Vector3fc center, float radius) {
        //hic sunt dracones
        Vector4f position = getCombined().transform(new Vector4f(center, 1));
        position.x /= position.w;
        position.y /= position.w;

        float boundingRadius = radius / max(1, abs(position.z)); //do not question how or why this works

        Vector2ic viewport = getViewportSize();
        float aspect = (float) viewport.x() / (float) viewport.y();

        //this does not actually work correctly, as the sphere should get warped from perspective projection - might
        //just add a bias if false culling becomes noticeable and call it a day though
        //also i just found out this does not even work correctly with a non-standard fov fml
        return !(position.z < getNear() - radius
                || position.z > getFar() + radius
                //FIXME stupid edge case where if center is behind camera but bounding radius still causes intersect
                // even if the center is already behind the near plane, this would flicker because x/y would briefly go
                // to infinity as perspective projection "flips" over - not perfect, since objects in normal range now
                // flicker if outside of ndc range when z goes from 1 to -1, but better than visible objects flickering
                || (abs(position.z) > 1)
                && (abs(position.x) > 1 + boundingRadius
                || abs(position.y) > 1 + (boundingRadius * aspect)));
    }

}
