package org.etieskrill.engine.entity.component;

import lombok.Data;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.data.DirectionalLight;
import org.etieskrill.engine.graphics.gl.framebuffer.DirectionalShadowMap;
import org.jetbrains.annotations.Nullable;

public @Data class DirectionalLightComponent {

    private final DirectionalLight directionalLight;
    private final @Nullable DirectionalShadowMap shadowMap;
    private final @Nullable Camera camera;

}
