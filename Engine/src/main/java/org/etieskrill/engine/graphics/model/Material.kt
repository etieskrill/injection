package org.etieskrill.engine.graphics.model

import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.etieskrill.engine.graphics.gl.shader.UniformMappable
import org.etieskrill.engine.graphics.texture.CubeMapTexture
import org.etieskrill.engine.graphics.texture.Texture2D
import org.joml.Vector4f
import org.joml.Vector4fc

private typealias Colour = Vector4fc

// phong:
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
    override val name: String? = null,
    override val isTwoSided: Boolean = false,

    val diffuseColour: Colour = Vector4f(1f), //FIXME what the hell is AI_MATKEY_BASE_COLOR then?
    val specularColour: Colour = Vector4f(1f),
    val ambientColour: Colour = Vector4f(1f),
    val shininess: Float = 256f,
    val shininessStrength: Float = 1f,
    val opacity: Float = 1f,
    val emissiveColour: Colour = Vector4f(0f),
    val emissiveStrength: Float = 0f,

    val diffuseTexture: Texture2D? = null,
    val specularTexture: Texture2D? = null,
    val normalTexture: Texture2D? = null,
    val heightTexture: Texture2D? = null,
    val emissiveTexture: Texture2D? = null,
    val ambientOcclusionTexture: Texture2D? = null
) : Material() {
    @Suppress("KotlinConstantConditions")
    override fun map(mapper: ShaderProgram.UniformMapper) = mapper.run {
        map("colour", diffuseColour)
        map("diffuseColour", diffuseColour)
        map("specularColour", specularColour)
        map("ambientColour", ambientColour)
        map("shininess", shininess)
        map("shininessStrength", shininessStrength)
        map("opacity", opacity)
        map("emissiveColour", emissiveColour)
        map("emissiveStrength", emissiveStrength)

        map("hasDiffuse", diffuseTexture != null)
        map("diffuse", diffuseTexture)
        map("hasSpecular", specularTexture != null)
        map("specular", specularTexture)
        map("hasNormal", normalTexture != null)
        map("normal", normalTexture)
        map("hasHeight", heightTexture != null)
        map("height", heightTexture)
        map("hasEmissive", emissiveTexture != null)
        map("emissive", emissiveTexture)
        map("hasAmbientOcclusion", ambientOcclusionTexture != null)
        map("ambientOcclusion", ambientOcclusionTexture)

        true
    }

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

data class PBRMaterial( //TODO defaults
    override val name: String? = null,
    override val isTwoSided: Boolean = false,

    val diffuseColour: Colour = Vector4f(1f),
    val opacity: Float? = null,
    val emissiveColour: Colour? = null,
    val emissiveStrength: Float? = null,

    val diffuseTexture: Texture2D? = null,
    val metallicTexture: Texture2D? = null, //TODO orm support
    val roughnessTexture: Texture2D? = null,
    val ambientOcclusionTexture: Texture2D? = null,
    val normalTexture: Texture2D? = null,
    val heightTexture: Texture2D? = null,
    val emissiveTexture: Texture2D? = null,
) : Material() {
    @Suppress("KotlinConstantConditions")
    override fun map(mapper: ShaderProgram.UniformMapper) = mapper.run {
        map("colour", diffuseColour)
        map("diffuseColour", diffuseColour)
        map("opacity", opacity)
        map("emissiveColour", emissiveColour)
        map("emissiveStrength", emissiveStrength)

        map("hasDiffuse", diffuseTexture != null)
        map("diffuse", diffuseTexture)
        map("hasMetallic", metallicTexture != null)
        map("metallic", metallicTexture)
        map("hasNormal", normalTexture != null)
        map("hasRoughness", roughnessTexture != null)
        map("roughness", roughnessTexture)
        map("hasAmbientOcclusion", ambientOcclusionTexture != null)
        map("ambientOcclusion", ambientOcclusionTexture)
        map("normal", normalTexture)
        map("hasHeight", heightTexture != null)
        map("height", heightTexture)
        map("hasEmissive", emissiveTexture != null)
        map("emissive", emissiveTexture)

        true
    }

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

data class SkyboxMaterial(
    override val name: String? = null,

    val skyboxTexture: CubeMapTexture? = null,
    val diffuseColour: Colour = Vector4f(0.25f),
    val opacity: Float? = null
) : Material() {

    override val isTwoSided: Boolean = false

    override fun map(mapper: ShaderProgram.UniformMapper): Boolean {
        mapper.map("skybox", skyboxTexture)
            .map("diffuseColour", diffuseColour)
            .map("opacity", opacity)
        return true
    }

    private var isDisposed = false
    override fun dispose() {
        if (isDisposed) return

        skyboxTexture?.dispose()

        isDisposed = true
    }
}

abstract class Material : UniformMappable, Disposable {

    abstract val name: String?
    abstract val isTwoSided: Boolean

}
