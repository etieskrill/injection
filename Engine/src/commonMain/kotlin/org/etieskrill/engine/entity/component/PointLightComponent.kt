package org.etieskrill.engine.entity.component

import net.bifreus.kml.Mat4c
import org.etieskrill.engine.graphics.data.PointLight
import org.etieskrill.engine.graphics.framebuffer.PointShadowMapArray

@ConsistentCopyVisibility
data class PointLightComponent private constructor(
    val light: PointLight,
    val shadowMap: PointShadowMapArray?,
    val shadowMapIndex: Int?,
    val shadowCombinedMatrices: Array<out Mat4c>?,
    val shadowFarPlane: Float?,
) {

    companion object {
        fun withoutShadowMaps(light: PointLight) = PointLightComponent(light, null, null, null, null)
        fun withShadowMaps(
            light: PointLight,
            shadowMap: PointShadowMapArray,
            shadowMapIndex: Int,
            shadowCombinedMatrices: Array<out Mat4c>,
            shadowFarPlane: Float
        ) = PointLightComponent(light, shadowMap, shadowMapIndex, shadowCombinedMatrices, shadowFarPlane)
    }

}
