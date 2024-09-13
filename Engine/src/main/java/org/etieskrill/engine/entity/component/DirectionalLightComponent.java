package org.etieskrill.engine.entity.component;

import lombok.Data;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.data.DirectionalLight;
import org.etieskrill.engine.graphics.gl.framebuffer.DirectionalShadowMap;

public @Data class DirectionalLightComponent {

    private final DirectionalLight directionalLight;
    private final DirectionalShadowMap shadowMap;
    private final Camera camera;

}
