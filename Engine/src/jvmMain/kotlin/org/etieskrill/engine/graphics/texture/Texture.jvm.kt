package org.etieskrill.engine.graphics.texture;

import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.gl.GLUtils
import org.joml.Vector2i
import org.joml.Vector2ic
import org.joml.Vector4fc
import org.lwjgl.BufferUtils
import org.lwjgl.assimp.Assimp.aiTextureType_AMBIENT_OCCLUSION
import org.lwjgl.assimp.Assimp.aiTextureType_DIFFUSE
import org.lwjgl.assimp.Assimp.aiTextureType_DIFFUSE_ROUGHNESS
import org.lwjgl.assimp.Assimp.aiTextureType_EMISSIVE
import org.lwjgl.assimp.Assimp.aiTextureType_HEIGHT
import org.lwjgl.assimp.Assimp.aiTextureType_METALNESS
import org.lwjgl.assimp.Assimp.aiTextureType_NORMALS
import org.lwjgl.assimp.Assimp.aiTextureType_SHININESS
import org.lwjgl.assimp.Assimp.aiTextureType_SPECULAR
import org.lwjgl.assimp.Assimp.aiTextureType_UNKNOWN
import org.lwjgl.opengl.GL11.GL_LINEAR
import org.lwjgl.opengl.GL11.GL_NEAREST
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL11C.GL_ALPHA
import org.lwjgl.opengl.GL11C.GL_BLUE
import org.lwjgl.opengl.GL11C.GL_DEPTH_COMPONENT
import org.lwjgl.opengl.GL11C.GL_GREEN
import org.lwjgl.opengl.GL11C.GL_LINEAR_MIPMAP_LINEAR
import org.lwjgl.opengl.GL11C.GL_LINEAR_MIPMAP_NEAREST
import org.lwjgl.opengl.GL11C.GL_NEAREST_MIPMAP_LINEAR
import org.lwjgl.opengl.GL11C.GL_NEAREST_MIPMAP_NEAREST
import org.lwjgl.opengl.GL11C.GL_ONE
import org.lwjgl.opengl.GL11C.GL_RED
import org.lwjgl.opengl.GL11C.GL_REPEAT
import org.lwjgl.opengl.GL11C.GL_RGB
import org.lwjgl.opengl.GL11C.GL_RGBA
import org.lwjgl.opengl.GL11C.GL_STENCIL_INDEX
import org.lwjgl.opengl.GL11C.GL_TEXTURE_BORDER_COLOR
import org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER
import org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER
import org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S
import org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T
import org.lwjgl.opengl.GL11C.glBindTexture
import org.lwjgl.opengl.GL11C.glDeleteTextures
import org.lwjgl.opengl.GL11C.glTexParameterfv
import org.lwjgl.opengl.GL11C.glTexParameteri
import org.lwjgl.opengl.GL11C.glTexParameteriv
import org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL12C.GL_TEXTURE_WRAP_R
import org.lwjgl.opengl.GL13C.GL_CLAMP_TO_BORDER
import org.lwjgl.opengl.GL13C.GL_TEXTURE0
import org.lwjgl.opengl.GL13C.glActiveTexture
import org.lwjgl.opengl.GL14C.GL_MIRRORED_REPEAT
import org.lwjgl.opengl.GL21C.GL_SRGB
import org.lwjgl.opengl.GL21C.GL_SRGB_ALPHA
import org.lwjgl.opengl.GL30C.GL_DEPTH_STENCIL
import org.lwjgl.opengl.GL30C.GL_RG
import org.lwjgl.opengl.GL30C.GL_RGBA16F
import org.lwjgl.opengl.GL30C.glGenerateMipmap
import org.lwjgl.opengl.GL33C.GL_TEXTURE_SWIZZLE_RGBA
import kotlin.properties.Delegates.notNull
import io.github.etieskrill.injection.extension.shader.Texture as DslTexture

/**
 * As this class makes use of the stb_image library, it can decode from all the image formats specified in the
 * official documentation: [stb_image](https://github.com/nothings/stb/blob/5736b15f7ea0ffb08dd38af21067c314d6a3aae9/stb_image.h#L23-L33).
 */
actual abstract class Texture actual constructor(
    actual val context: GraphicsContext,
    actual val type: TextureType,
    format: TextureFormat?,
    minFilter: TextureMinFilter,
    magFilter: TextureMagFilter,
    wrapping: TextureWrapping,
    borderColour: Vector4fc
) : DslTexture, Disposable {

    internal var _format: TextureFormat by notNull()
    actual val format: TextureFormat get() = _format

    var wrapping: TextureWrapping = wrapping
        set(value) {
            bind(0)
            context.withContext {
                glTexParameteri(glTarget, GL_TEXTURE_WRAP_S, value.gl)
                glTexParameteri(glTarget, GL_TEXTURE_WRAP_T, value.gl)
                glTexParameteri(glTarget, GL_TEXTURE_WRAP_R, value.gl)
            }
        }

    protected abstract val glTarget: Int

    protected val id: Int by lazy { context.withContext { GL11C.glGenTextures() } }

    protected abstract fun bufferTextureData()

    init {
        context.withContext {
            bind(0)

            glTexParameteri(glTarget, GL_TEXTURE_MIN_FILTER, minFilter.gl)
            glTexParameteri(glTarget, GL_TEXTURE_MAG_FILTER, magFilter.gl)

            this.wrapping = wrapping

            glTexParameterfv(glTarget, GL_TEXTURE_BORDER_COLOR, borderColour[BufferUtils.createFloatBuffer(4)])

            val swizzleMask = when (this.format) {
                TextureFormat.GRAY, TextureFormat.DEPTH, TextureFormat.STENCIL, TextureFormat.DEPTH_STENCIL
                     -> intArrayOf(GL_RED, GL_RED, GL_RED, GL_ONE)
                TextureFormat.ALPHA -> intArrayOf(GL_ONE, GL_ONE, GL_ONE, GL_ALPHA, GL_RED)
                TextureFormat.GRAY_ALPHA -> intArrayOf(GL_RED, GL_RED, GL_RED, GL_GREEN)
                TextureFormat.RGB, TextureFormat.SRGB -> intArrayOf(GL_RED, GL_GREEN, GL_BLUE, GL_ONE)
                TextureFormat.RGBA, TextureFormat.SRGBA, TextureFormat.RGBA_HDR
                     -> intArrayOf(GL_RED, GL_GREEN, GL_BLUE, GL_ALPHA)
            }

            glTexParameteriv(glTarget, GL_TEXTURE_SWIZZLE_RGBA, swizzleMask)

            bufferTextureData()
            GLUtils.checkErrorThrowing("Error while buffering texture data: $this")

            if (minFilter in setOf(TextureMinFilter.NEAREST, TextureMinFilter.LINEAR)
                && format !in setOf(TextureFormat.DEPTH, TextureFormat.STENCIL, TextureFormat.DEPTH_STENCIL)
            ) {
                glGenerateMipmap(glTarget)
            }

            GLUtils.checkErrorThrowing("Error while creating texture: $this")
        }
    }

    fun bind(unit: Int) = context.withContext {
        check(unit < context.maxTextureUnits) { "Texture unit $unit is not supported" }
        glActiveTexture(GL_TEXTURE0 + unit)
        glBindTexture(glTarget, id)
    }

    fun unbind(unit: Int) = context.withContext {
        glActiveTexture(GL_TEXTURE0 + unit)
        glBindTexture(glTarget, 0)
    }

    private var wasAlreadyDisposed = false

    actual override fun dispose() {
        if (wasAlreadyDisposed) return
        context.withContext { glDeleteTextures(id) }
        wasAlreadyDisposed = true
    }

    override fun toString(): String {
        return "Texture(type=$type, format=$format)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Texture

        if (context != other.context) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + id
        return result
    }

}

internal val TextureFormat.gl get() = when (this) {
    TextureFormat.ALPHA -> GL_RED
    TextureFormat.GRAY -> GL_RED
    TextureFormat.GRAY_ALPHA -> GL_RG
    TextureFormat.RGB -> GL_RGB
    TextureFormat.RGBA -> GL_RGBA
    TextureFormat.RGBA_HDR -> GL_RGBA
    TextureFormat.SRGB -> GL_RGB
    TextureFormat.SRGBA -> GL_RGBA
    TextureFormat.DEPTH -> GL_DEPTH_COMPONENT
    TextureFormat.STENCIL -> GL_STENCIL_INDEX
    TextureFormat.DEPTH_STENCIL -> GL_DEPTH_STENCIL
}

internal val TextureFormat.glInternal get() = when (this) {
    TextureFormat.ALPHA -> GL_RED
    TextureFormat.GRAY -> GL_RED
    TextureFormat.GRAY_ALPHA -> GL_RG
    TextureFormat.RGB -> GL_RGB
    TextureFormat.RGBA -> GL_RGBA
    TextureFormat.RGBA_HDR -> GL_RGBA16F
    TextureFormat.SRGB -> GL_SRGB
    TextureFormat.SRGBA -> GL_SRGB_ALPHA
    TextureFormat.DEPTH -> GL_DEPTH_COMPONENT
    TextureFormat.STENCIL -> GL_STENCIL_INDEX
    TextureFormat.DEPTH_STENCIL -> GL_DEPTH_STENCIL
}

internal fun textureFormatFromNumChannels(numChannels: Int): TextureFormat = when (numChannels) {
    1 -> TextureFormat.GRAY
    2 -> TextureFormat.GRAY_ALPHA
    3 -> TextureFormat.SRGB
    4 -> TextureFormat.SRGBA
    else -> error("Unexpected number of channels: $numChannels")
}

internal fun textureFormatFromNumChannelsAndType(
    numChannels: Int,
    type: TextureType
): TextureFormat = when (numChannels) {
    1 -> TextureFormat.GRAY
    2 -> TextureFormat.GRAY_ALPHA
    3 -> if (type == TextureType.DIFFUSE) TextureFormat.SRGB else TextureFormat.RGB
    4 -> if (type == TextureType.DIFFUSE) TextureFormat.SRGBA else TextureFormat.RGBA
    else -> error("Unexpected number of channels: $numChannels")
}

internal val TextureMinFilter.gl get() = when (this) {
    TextureMinFilter.NEAREST -> GL_NEAREST
    TextureMinFilter.LINEAR -> GL_LINEAR
    TextureMinFilter.NEAREST_NEAREST -> GL_NEAREST_MIPMAP_NEAREST
    TextureMinFilter.BILINEAR -> GL_LINEAR_MIPMAP_NEAREST
    TextureMinFilter.NEAREST_LINEAR -> GL_NEAREST_MIPMAP_LINEAR
    TextureMinFilter.TRILINEAR -> GL_LINEAR_MIPMAP_LINEAR
}

internal val TextureMagFilter.gl get() = when (this) {
    TextureMagFilter.NEAREST -> GL_NEAREST
    TextureMagFilter.LINEAR -> GL_LINEAR
}

internal val TextureWrapping.gl get() = when (this) {
    TextureWrapping.REPEAT -> GL_REPEAT
    TextureWrapping.MIRRORED_REPEAT -> GL_MIRRORED_REPEAT
    TextureWrapping.CLAMP_TO_EDGE -> GL_CLAMP_TO_EDGE
    TextureWrapping.CLAMP_TO_BORDER -> GL_CLAMP_TO_BORDER
}

//TODO move to model/scene loader?
internal val TextureType.ai get() = when (this) {
    TextureType.UNKNOWN -> aiTextureType_UNKNOWN
    TextureType.DIFFUSE -> aiTextureType_DIFFUSE
    TextureType.SPECULAR -> aiTextureType_SPECULAR
    TextureType.SHININESS -> aiTextureType_SHININESS
    TextureType.HEIGHT -> aiTextureType_HEIGHT
    TextureType.EMISSIVE -> aiTextureType_EMISSIVE
    TextureType.NORMAL -> aiTextureType_NORMALS
    TextureType.METALNESS -> aiTextureType_METALNESS
    TextureType.ROUGHNESS -> aiTextureType_DIFFUSE_ROUGHNESS
    TextureType.AMBIENT_OCCLUSION -> aiTextureType_AMBIENT_OCCLUSION

    TextureType.SHADOW, TextureType.G_POSITION, TextureType.G_DEPTH, TextureType.G_COLOUR, TextureType.G_NORMAL -> null
}
