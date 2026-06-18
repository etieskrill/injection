package org.etieskrill.engine.graphics.model.loader

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.config.TEXTURE_PATH
import org.etieskrill.engine.graphics.model.Material
import org.etieskrill.engine.graphics.model.PBRMaterial
import org.etieskrill.engine.graphics.model.PhongMaterial
import org.etieskrill.engine.graphics.texture.AbstractTexture
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.graphics.texture.Textures
import org.etieskrill.engine.time.StepTimer
import org.etieskrill.engine.util.ResourceReader.classpathResourceExists
import org.etieskrill.engine.util.name
import org.joml.Vector2i
import org.joml.Vector4f
import org.joml.Vector4fc
import org.lwjgl.BufferUtils
import org.lwjgl.assimp.AIMaterial
import org.lwjgl.assimp.AIMaterialProperty
import org.lwjgl.assimp.AIScene
import org.lwjgl.assimp.AIString
import org.lwjgl.assimp.AITexture
import org.lwjgl.assimp.Assimp.*
import org.lwjgl.stb.STBImage.stbi_failure_reason
import org.lwjgl.stb.STBImage.stbi_load_from_memory

private val logger = KotlinLogging.logger("MaterialLoader")
private val timer = StepTimer(logger)

internal fun loadEmbeddedTextures(scene: AIScene): Map<String, Texture2D> {
    val embeddedTextures = mutableMapOf<String, Texture2D>()
    val textures = scene.mTextures()
        ?: if (scene.mNumTextures() == 0) return embeddedTextures
        else error("Texture array is not available but number of textures is not zero")
    for (i in 0..<scene.mNumTextures()) {
        val texture = AITexture.create(textures.get(i))

        timer.start()

        val width = IntArray(1)
        val height = IntArray(1)
        val channels = IntArray(1)
        val imageData = stbi_load_from_memory(texture.pcDataCompressed(), width, height, channels, 0)
        if (imageData == null) {
            logger.warn { "Failed to decode embedded texture $i: ${stbi_failure_reason()}" }
            continue
        }

        timer.trace { "Embedded texture decode" }

        val filePath = texture.mFilename().dataString()
        val tex = Texture2D.BufferBuilder(
            imageData, Vector2i(width[0], height[0]), AbstractTexture.Format.fromChannels(channels[0])
        ).setType(determineType(filePath)).build()

        embeddedTextures["*$i"] = tex
        embeddedTextures[filePath] = tex

        timer.trace { "Loaded embedded texture $i $filePath: $tex" }
    }

    logger.debug { "${embeddedTextures.size / 2} of ${scene.mNumTextures()} embedded textures loaded" }
    return embeddedTextures
}

private fun determineType(filePath: String): AbstractTexture.Type = when {
    "diffuse" in filePath.name -> AbstractTexture.Type.DIFFUSE
    "specular" in filePath.name -> AbstractTexture.Type.SPECULAR
    "normal" in filePath.name -> AbstractTexture.Type.NORMAL
    "emissive" in filePath.name -> AbstractTexture.Type.EMISSIVE
    else -> AbstractTexture.Type.UNKNOWN
}

internal fun loadMaterials(
    scene: AIScene,
    materials: MutableList<Material>,
    embeddedTextures: Map<String, Texture2D>,
    modelName: String
) {
    timer.start()
    logger.debug { "${scene.mNumMaterials()} materials found" }
    val aiMaterials = scene.mMaterials()
        ?: if (scene.mNumMaterials() == 0) return else error("Material array is not available but number of materials is not zero")
    for (i in 0..<scene.mNumMaterials()) {
        timer.trace { "Processing material $i" }
        materials += processMaterial(i, AIMaterial.create(aiMaterials.get()), embeddedTextures, modelName)
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun processMaterial(
    materialIndex: Int,
    aiMaterial: AIMaterial,
    embeddedTextures: Map<String, Texture2D>,
    modelName: String
): Material = when (val model = aiMaterial.getIntProperty(AI_MATKEY_SHADING_MODEL)) {
    aiShadingMode_Flat, aiShadingMode_Gouraud, aiShadingMode_Phong, aiShadingMode_Blinn, aiShadingMode_Toon ->
        processPhongMaterial(materialIndex, aiMaterial, embeddedTextures, modelName)

    aiShadingMode_CookTorrance, aiShadingMode_PBR_BRDF ->
        processPBRMaterial(materialIndex, aiMaterial, embeddedTextures, modelName)

    null -> error("Shading model property not found in material")
    else -> error("Unsupported shading model: 0x${model.toHexString()}")
}

private fun processPhongMaterial(
    materialIndex: Int,
    aiMaterial: AIMaterial,
    embeddedTextures: Map<String, Texture2D>,
    modelName: String
): PhongMaterial = PhongMaterial(
    name = aiMaterial.getStringProperty(AI_MATKEY_NAME),
    isTwoSided = aiMaterial.getBooleanProperty(AI_MATKEY_TWOSIDED) ?: false,

    diffuseColour = aiMaterial.getColourProperty(AI_MATKEY_COLOR_DIFFUSE) ?: Vector4f(1f),
    specularColour = aiMaterial.getColourProperty(AI_MATKEY_COLOR_SPECULAR),
    ambientColour = aiMaterial.getColourProperty(AI_MATKEY_COLOR_AMBIENT),
    shininess = aiMaterial.getFloatProperty(AI_MATKEY_SHININESS),
    shininessStrength = aiMaterial.getFloatProperty(AI_MATKEY_SHININESS_STRENGTH),
    opacity = aiMaterial.getFloatProperty(AI_MATKEY_OPACITY),
    emissiveColour = aiMaterial.getColourProperty(AI_MATKEY_COLOR_EMISSIVE),
    emissiveStrength = aiMaterial.getFloatProperty(AI_MATKEY_EMISSIVE_INTENSITY),

    diffuseTexture = aiMaterial.getTexture(AbstractTexture.Type.DIFFUSE, materialIndex, modelName, embeddedTextures),
    specularTexture = aiMaterial.getTexture(AbstractTexture.Type.SPECULAR, materialIndex, modelName, embeddedTextures),
    normalTexture = aiMaterial.getTexture(AbstractTexture.Type.NORMAL, materialIndex, modelName, embeddedTextures),
    heightTexture = aiMaterial.getTexture(AbstractTexture.Type.HEIGHT, materialIndex, modelName, embeddedTextures),
    emissiveTexture = aiMaterial.getTexture(AbstractTexture.Type.EMISSIVE, materialIndex, modelName, embeddedTextures),
    ambientOcclusionTexture = aiMaterial.getTexture(
        AbstractTexture.Type.AMBIENT_OCCLUSION,
        materialIndex,
        modelName,
        embeddedTextures
    )
)

private fun processPBRMaterial(
    materialIndex: Int,
    aiMaterial: AIMaterial,
    embeddedTextures: Map<String, Texture2D>,
    modelName: String
): PBRMaterial = PBRMaterial(
    name = aiMaterial.getStringProperty(AI_MATKEY_NAME),
    isTwoSided = aiMaterial.getBooleanProperty(AI_MATKEY_TWOSIDED) ?: false,

    diffuseColour = aiMaterial.getColourProperty(AI_MATKEY_COLOR_DIFFUSE) ?: Vector4f(1f),
    opacity = aiMaterial.getFloatProperty(AI_MATKEY_OPACITY),
    emissiveColour = aiMaterial.getColourProperty(AI_MATKEY_COLOR_EMISSIVE),
    emissiveStrength = aiMaterial.getFloatProperty(AI_MATKEY_EMISSIVE_INTENSITY),

    diffuseTexture = aiMaterial.getTexture(AbstractTexture.Type.DIFFUSE, materialIndex, modelName, embeddedTextures),
    metallicTexture = aiMaterial.getTexture(AbstractTexture.Type.METALNESS, materialIndex, modelName, embeddedTextures),
    roughnessTexture = aiMaterial.getTexture(
        AbstractTexture.Type.ROUGHNESS,
        materialIndex,
        modelName,
        embeddedTextures
    ),
    ambientOcclusionTexture = aiMaterial.getTexture(
        AbstractTexture.Type.AMBIENT_OCCLUSION,
        materialIndex,
        modelName,
        embeddedTextures
    ),
    normalTexture = aiMaterial.getTexture(AbstractTexture.Type.NORMAL, materialIndex, modelName, embeddedTextures),
    heightTexture = aiMaterial.getTexture(AbstractTexture.Type.HEIGHT, materialIndex, modelName, embeddedTextures),
    emissiveTexture = aiMaterial.getTexture(AbstractTexture.Type.EMISSIVE, materialIndex, modelName, embeddedTextures)
)

private fun AIMaterial.getBooleanProperty(property: String): Boolean? =
    getProperty(property, aiPTI_Integer, 1)?.mData()?.get()?.let { it > 0 }

private fun AIMaterial.getIntProperty(property: String): Int? =
    getProperty(property, aiPTI_Integer, Int.SIZE_BYTES)?.mData()?.int

private fun AIMaterial.getFloatProperty(property: String): Float? =
    getProperty(property, aiPTI_Float, Float.SIZE_BYTES)?.mData()?.float

private fun AIMaterial.getColourProperty(property: String): Vector4fc? {
    val data = getProperty(property, aiPTI_Float)?.mData() ?: return null
    return if (data.remaining() == 3 * Float.SIZE_BYTES) Vector4f(data.float, data.float, data.float, 1f)
    else if (data.remaining() == 4 * Float.SIZE_BYTES) Vector4f(data.float, data.float, data.float, data.float)
    else error("Colour property contained ${data.remaining() / Float.SIZE_BYTES} values where 3/4 are required")
}

private fun AIMaterial.getStringProperty(property: String): String? {
    val property = getProperty(property, aiPTI_String) ?: return null
    val data = property.mData()
    return String(ByteArray(property.mDataLength()) { data.get() })
}

@OptIn(ExperimentalStdlibApi::class)
private fun AIMaterial.getProperty(property: String, type: Int, length: Int? = null): AIMaterialProperty? {
    val propertyBuffer = BufferUtils.createPointerBuffer(1)
    if (aiReturn_SUCCESS != aiGetMaterialProperty(this, property, propertyBuffer)) {
        logger.debug { "Failed to get property $property: ${aiGetErrorString()}" }
        return null
    }
    val property = AIMaterialProperty.create(propertyBuffer.get())

    check(property.mType() == type || property.mType() == aiPTI_Buffer) {
        "Property has type 0x${property.mType().toHexString()}, not 0x${type.toHexString()}"
    }
    check(length == null || property.mDataLength() == length) {
        "Property has ${property.mDataLength()} instead of the required $length elements"
    }
    return property
}

private fun AIMaterial.getTexture(
    type: AbstractTexture.Type,
    materialIndex: Int,
    modelName: String,
    embeddedTextures: Map<String, Texture2D>
): Texture2D? {
    val file = AIString.create()
    val textureCount = aiGetMaterialTextureCount(this, type.ai())
    if (1 != textureCount) {
        logger.debug { "Texture of type '$type' is not loaded because material texture count was $textureCount" }
        return null
    }

    if (aiReturn_SUCCESS != aiGetMaterialTexture(this, type.ai(), 0, file, IntArray(1), null, null, null, null, null)) {
        logger.debug { "Error while loading material texture: ${aiGetErrorString()}" }
        return null
    }

    val textureName = "${modelName}_mat${materialIndex}_${type.name.lowercase()}"
    val textureFile = file.dataString()

    return when {
        textureFile in embeddedTextures -> {
            logger.trace { "Texture $textureName is loaded from embedded textures" }
            embeddedTextures[textureFile]!!.apply {
//                format = AbstractTexture.Format.fromChannelsAndType(type) //FIXME
//                this.type = type
            }
        }

        classpathResourceExists(textureFile) -> {
            logger.trace { "Texture $textureName is loaded from file $textureFile" }
            Textures.ofFile(textureFile, type)
        }

        classpathResourceExists("$TEXTURE_PATH/${textureFile.name}") -> {
            logger.debug { "Texture $textureName is loaded as fallback based on file name from file $textureFile" }
            Textures.ofFile("$TEXTURE_PATH/${textureFile.name}", type)
        }

        embeddedTextures.any { it.value.type == type } -> {
            logger.debug { "Texture $textureName is loaded as fallback from embedded textures based on texture type" }
            embeddedTextures.values.single { it.type == type }
        }

        else -> {
            logger.warn { "Failed to find any texture for '$textureName' at $textureFile" }
            null
        }
    }
}
