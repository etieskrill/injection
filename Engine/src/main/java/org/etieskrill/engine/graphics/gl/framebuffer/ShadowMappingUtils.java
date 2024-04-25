package org.etieskrill.engine.graphics.gl.framebuffer;

import org.etieskrill.engine.graphics.data.PointLight;
import org.etieskrill.engine.graphics.texture.CubeMapTexture;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector2ic;
import org.joml.Vector3f;

import static java.lang.Math.toRadians;

class ShadowMappingUtils {

    //TODO better local caching strategy - or just stack allocated matrices, but fucking how
    private static final Matrix4f projectionCached = new Matrix4f();
    private static final Vector3f lightPositionCached = new Vector3f();

    static synchronized Matrix4f[] getCombinedMatrices(Vector2ic size, float near, float far, @NotNull PointLight light, @NotNull Matrix4f[] targets) {
        projectionCached.setPerspective((float) toRadians(90), (float) size.x() / size.y(), near, far);
        for (int i = 0; i < CubeMapTexture.NUM_SIDES; i++) {
            targets[i].lookAt(light.getPosition(),
                    lightPositionCached.set(light.getPosition()).add(CubeMapTexture.FACE_NORMALS[i]),
                    CubeMapTexture.FACE_UPS[i]);
            projectionCached.mul(targets[i], targets[i]);
        }
        return targets;
    }

    private ShadowMappingUtils() {
        //Not intended for instantiation
    }

}
