package org.etieskrill.engine.entity.component;

import org.etieskrill.engine.graphics.data.DirectionalLight;
import org.etieskrill.engine.graphics.gl.framebuffer.DirectionalShadowMap;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class DirectionalLightComponent {

    private final DirectionalLight directionalLight;
    private final @Nullable DirectionalShadowMap shadowMap;
    private final @Nullable Matrix4f combined;

    public DirectionalLightComponent(DirectionalLight directionalLight) {
        this(directionalLight, null, null);
    }

    public DirectionalLightComponent(DirectionalLight directionalLight, @Nullable DirectionalShadowMap shadowMap, @Nullable Matrix4f combined) {
        this.directionalLight = directionalLight;
        this.shadowMap = shadowMap;
        this.combined = combined;
    }

    public DirectionalLight getDirectionalLight() {
        return directionalLight;
    }

    public @Nullable DirectionalShadowMap getShadowMap() {
        return shadowMap;
    }

    public @Nullable Matrix4f getCombined() {
        return combined;
    }

    public void setCombined(Matrix4fc combined) {
        //TODO Matrix4f#test* methods could make things quite a lot simpler for frustum culling n some other stuff
        if (this.combined != null) {
            this.combined.set(combined);
        }
    }

}
