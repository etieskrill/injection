package org.etieskrill.engine.graphics.model

import org.etieskrill.engine.graphics.shader.UniformMappable
import org.etieskrill.engine.graphics.shader.UniformMapper
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.graphics.texture.TextureCubeMap
import org.etieskrill.engine.graphics.texture.TextureType
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

abstract class Material : UniformMappable {

    abstract val name: String?
    abstract val isTwoSided: Boolean

}

data class PhongMaterial(
    override val name: String? = null,
    override val isTwoSided: Boolean = false,

    val textureScale: Float = 1f,

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
    val ambientOcclusionTexture: Texture2D? = null,

    val hasDiffuseTexture: Boolean = diffuseTexture != null,
    val hasSpecularTexture: Boolean = specularTexture != null,
    val hasNormalTexture: Boolean = normalTexture != null,
    val hasHeightTexture: Boolean = heightTexture != null,
    val hasEmissiveTexture: Boolean = emissiveTexture != null,
    val hasAmbientOcclusionTexture: Boolean = ambientOcclusionTexture != null,
) : Material() {
    init {
        require(diffuseTexture == null || diffuseTexture.type == TextureType.DIFFUSE) { "Diffuse texture must have DIFFUSE type" }
        require(specularTexture == null || specularTexture.type == TextureType.SPECULAR || specularTexture.type == TextureType.ROUGHNESS) { "Specular texture must have SPECULAR or ROUGHNESS type" }
        require(normalTexture == null || normalTexture.type == TextureType.NORMAL) { "Normal texture must have NORMAL type" }
        require(heightTexture == null || heightTexture.type == TextureType.HEIGHT) { "Height texture must have HEIGHT type" }
        require(emissiveTexture == null || emissiveTexture.type == TextureType.EMISSIVE) { "Emissive texture must have EMISSIVE type" }
        require(ambientOcclusionTexture == null || ambientOcclusionTexture.type == TextureType.AMBIENT_OCCLUSION) { "Ambient occlusion texture must have AMBIENT_OCCLUSION type" }
    }

    @Suppress("KotlinConstantConditions")
    override fun map(mapper: UniformMapper) = mapper.run {
        map("textureScale", textureScale)

        map("colour", diffuseColour)
        map("diffuseColour", diffuseColour)
        map("specularColour", specularColour)
        map("ambientColour", ambientColour)
        map("shininess", shininess)
        map("shininessStrength", shininessStrength)
        map("opacity", opacity)
        map("emissiveColour", emissiveColour)
        map("emissiveStrength", emissiveStrength)

        map("hasDiffuse", hasDiffuseTexture)
        map("diffuse", diffuseTexture)
        map("hasSpecular", hasSpecularTexture)
        map("specular", specularTexture)
        map("hasNormal", hasNormalTexture)
        map("normal", normalTexture)
        map("hasHeight", hasHeightTexture)
        map("height", heightTexture)
        map("hasEmissive", hasEmissiveTexture)
        map("emissive", emissiveTexture)
        map("hasAmbientOcclusion", hasAmbientOcclusionTexture)
        map("ambientOcclusion", ambientOcclusionTexture)

        true
    }
}

data class PBRMaterial(
    //TODO defaults
    override val name: String? = null,
    override val isTwoSided: Boolean = false,

    val textureScale: Float = 1f,

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
    init {
        require(diffuseTexture == null || diffuseTexture.type == TextureType.DIFFUSE) { "Diffuse texture must have DIFFUSE type" }
        require(metallicTexture == null || metallicTexture.type == TextureType.METALNESS) { "Metallic texture must have METALNESS type" }
        require(roughnessTexture == null || roughnessTexture.type == TextureType.ROUGHNESS || roughnessTexture.type == TextureType.SPECULAR) { "Roughness texture must have ROUGHNESS or SPECULAR type" }
        require(ambientOcclusionTexture == null || ambientOcclusionTexture.type == TextureType.AMBIENT_OCCLUSION) { "Ambient occlusion texture must have AMBIENT_OCCLUSION type" }
        require(normalTexture == null || normalTexture.type == TextureType.NORMAL) { "Normal texture must have NORMAL type" }
        require(heightTexture == null || heightTexture.type == TextureType.HEIGHT) { "Height texture must have HEIGHT type" }
        require(emissiveTexture == null || emissiveTexture.type == TextureType.EMISSIVE) { "Emissive texture must have EMISSIVE type" }
    }

    @Suppress("KotlinConstantConditions")
    override fun map(mapper: UniformMapper) = mapper.run {
        map("textureScale", textureScale)

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
}

// -2. @UniformStruct- (actually redundant because it can be inferred based on supertype UniformMappable)
data class SkyboxMaterial(
    /* 2. @Uniform(exclude = true) */ override val name: String? = null,

    /* 1. @Uniform */ val skyboxTexture: TextureCubeMap? = null,
    /* 1. @Uniform */ val diffuseColour: Colour = Vector4f(0.25f),
    /* 1. @Uniform */ val opacity: Float? = null
) : Material() {
    override val isTwoSided: Boolean = false

    init {
        require(skyboxTexture == null || skyboxTexture.type == TextureType.DIFFUSE) { "Skybox texture must have DIFFUSE type" }
    }

    override fun map(mapper: UniformMapper): Boolean {
        mapper.map("skybox", skyboxTexture)
            .map("diffuseColour", diffuseColour)
            .map("opacity", opacity)
        return true
    }
}
