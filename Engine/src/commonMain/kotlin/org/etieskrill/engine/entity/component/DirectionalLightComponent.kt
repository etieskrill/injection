package org.etieskrill.engine.entity.component

import org.etieskrill.engine.graphics.camera.Camera
import org.etieskrill.engine.graphics.data.DirectionalLight
import org.etieskrill.engine.graphics.gl.framebuffer.DirectionalShadowMap

data class DirectionalLightComponent(
    val directionalLight: DirectionalLight,
    val shadowMap: DirectionalShadowMap? = null,
    val camera: Camera? = null
)
