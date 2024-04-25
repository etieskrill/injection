package org.etieskrill.engine.entity.component;

import org.etieskrill.engine.graphics.data.PointLight;
import org.etieskrill.engine.graphics.gl.framebuffer.PointShadowMapArray;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

public class PointLightComponent {

    private final PointLight light;
    private final @Nullable PointShadowMapArray shadowMap;
    private final @Nullable Integer shadowMapIndex;
    private final @Nullable Matrix4fc[] combinedMatrices;
    private final @Nullable Float farPlane;

    public PointLightComponent(PointLight light, @Nullable PointShadowMapArray shadowMap, int shadowMapIndex, Matrix4fc[] combinedMatrices, float farPlane) {
        this.light = light;
        this.shadowMap = shadowMap;
        this.shadowMapIndex = shadowMapIndex;
        this.combinedMatrices = combinedMatrices;
        this.farPlane = farPlane;
    }

    public PointLight getLight() {
        return light;
    }

    public @Nullable PointShadowMapArray getShadowMap() {
        return shadowMap;
    }

    public @Nullable Integer getShadowMapIndex() {
        return shadowMapIndex;
    }

    public Matrix4fc[] getCombinedMatrices() {
        return combinedMatrices;
    }

    public @Nullable Float getFarPlane() {
        return farPlane;
    }

}
