package org.etieskrill.engine.graphics.model

import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.texture.Texture2D
import org.joml.Vector4fc

private typealias Colour = Vector4fc

// phong:
//
// properties:
// - diffuse/albedo/base colour: vec3 ]0,1[
// - specular colour: vec3 ]0,1[
// - ambient colour: vec3 ]0,1[
// - shininess: float ]0,x] - specular exponent
// - shininess strength: float ]0,1[ - flat specular multiplier
// - opacity: float ]0,1[
// - emissive colour: vec3 ]0,1[
// - emissive strength: float ]0,x[
// textures:
// - diffuse
// - specular
// - normal
// - height
// - emissive
// - ambient occlusion
//
// pbr:
// properties:
// - diffuse/albedo/base colour: vec3 ]0,1[
// - opacity: float ]0,1[
// - emissive colour: vec3 ]0,1[
// - emissive strength: float ]0,x[
// textures:
// - diffuse/albedo/base colour
// - metallic           |
// - roughness          |
// - ambient occlusion  > usually in one texture as bgr (ao/r/m (orm) in correct order)
// - normal
// - height/displacement
// - emissive
// - (sheen, transmission, aniso, aniso rotation, mask/alpha/opacity... it just does not end)

data class PhongMaterial(
    override val name: String?,
    override val isTwoSided: Boolean?,
    override val isWireframeEnabled: Boolean?,

    val diffuseColour: Colour?, //FIXME what the hell is AI_MATKEY_BASE_COLOR then?
    val specularColour: Colour?,
    val ambientColour: Colour?,
    val shininess: Float?,
    val shininessStrength: Float?,
    val opacity: Float?,
    val emissiveColour: Colour?,
    val emissiveStrength: Float?,

    val diffuseTexture: Texture2D?,
    val specularTexture: Texture2D?,
    val normalTexture: Texture2D?,
    val heightTexture: Texture2D?,
    val emissiveTexture: Texture2D?,
    val ambientOcclusionTexture: Texture2D?
) : Material() {
    private var isDisposed = false

    override fun dispose() {
        if (isDisposed) return

        diffuseTexture?.dispose()
        specularTexture?.dispose()
        normalTexture?.dispose()
        heightTexture?.dispose()
        emissiveTexture?.dispose()
        ambientOcclusionTexture?.dispose()

        isDisposed = true
    }
}

data class PBRMaterial(
    override val name: String?,
    override val isTwoSided: Boolean?,
    override val isWireframeEnabled: Boolean?,

    val diffuseColour: Colour?,
    val opacity: Float?,
    val emissiveColour: Colour?,
    val emissiveStrength: Float?,

    val diffuseTexture: Texture2D?,
    val metallicTexture: Texture2D?, //TODO orm support
    val roughnessTexture: Texture2D?,
    val ambientOcclusionTexture: Texture2D?,
    val normalTexture: Texture2D?,
    val heightTexture: Texture2D?,
    val emissiveTexture: Texture2D?,
) : Material() {
    private var isDisposed = false

    override fun dispose() {
        if (isDisposed) return

        diffuseTexture?.dispose()
        metallicTexture?.dispose()
        roughnessTexture?.dispose()
        ambientOcclusionTexture?.dispose()
        normalTexture?.dispose()
        heightTexture?.dispose()
        emissiveTexture?.dispose()

        isDisposed = true
    }
}

abstract class Material : Disposable {

    abstract val name: String?
    abstract val isTwoSided: Boolean?
    abstract val isWireframeEnabled: Boolean?

}
