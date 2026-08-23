package org.etieskrill.engine.entity.component

import org.etieskrill.engine.graphics.data.PointLight
import org.etieskrill.engine.graphics.gl.framebuffer.PointShadowMapArray
import org.joml.Matrix4fc

@ConsistentCopyVisibility
data class PointLightComponent private constructor(
    val light: PointLight,
    val shadowMap: PointShadowMapArray?,
    val shadowMapIndex: Int?,
    val shadowCombinedMatrices: Array<out Matrix4fc>?,
    val shadowFarPlane: Float?,
) {

    companion object {
        fun withoutShadowMaps(light: PointLight) = PointLightComponent(light, null, null, null, null)
        fun withShadowMaps(
            light: PointLight,
            shadowMap: PointShadowMapArray,
            shadowMapIndex: Int,
            shadowCombinedMatrices: Array<out Matrix4fc>,
            shadowFarPlane: Float
        ) = PointLightComponent(light, shadowMap, shadowMapIndex, shadowCombinedMatrices, shadowFarPlane)
    }

}
