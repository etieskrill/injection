package org.etieskrill.engine.graphics.texture;

import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.gl.GLUtils
import org.etieskrill.engine.util.ResourceReader
import org.joml.Vector2i
import org.lwjgl.BufferUtils
import org.lwjgl.BufferUtils.createIntBuffer
import org.lwjgl.assimp.Assimp.*
import org.lwjgl.opengl.GL11.GL_LINEAR
import org.lwjgl.opengl.GL11.GL_NEAREST
import org.lwjgl.opengl.GL11C.*
import org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL13C.GL_CLAMP_TO_BORDER
import org.lwjgl.opengl.GL14C.GL_MIRRORED_REPEAT
import org.lwjgl.opengl.GL21C.GL_SRGB
import org.lwjgl.opengl.GL21C.GL_SRGB_ALPHA
import org.lwjgl.opengl.GL30C.*
import org.lwjgl.opengl.GL33C.GL_TEXTURE_SWIZZLE_RGBA
import org.lwjgl.stb.STBImage.*
import java.util.*

/**
 * As this class makes use of the stb_image library, it can decode from all the image formats specified in the
 * official documentation: [stb_image](https://github.com/nothings/stb/blob/5736b15f7ea0ffb08dd38af21067c314d6a3aae9/stb_image.h#L23-L33).
 */
internal actual abstract class TextureInstance<T : Texture>(
    actual val descriptor: T,
    actual val context: GraphicsContext
) : Disposable {

    internal actual var version: Long = 0L

    protected abstract val glTarget: Int

    protected val id: Int = glGenTextures()

    protected abstract fun bufferTextureData()

    init {
        bind(0)

        glTexParameteri(glTarget, GL_TEXTURE_MIN_FILTER, descriptor.minFilter.gl)
        glTexParameteri(glTarget, GL_TEXTURE_MAG_FILTER, descriptor.magFilter.gl)

        glTexParameterfv(glTarget, GL_TEXTURE_BORDER_COLOR, descriptor.borderColour[BufferUtils.createFloatBuffer(4)])

        glPixelStorei(
            GL_UNPACK_ALIGNMENT, when (descriptor.rowAlignment) {
                TextureRowAlignment.BYTE -> 1
                TextureRowAlignment.EVEN_BYTE -> 2
                TextureRowAlignment.WORD -> 4
                TextureRowAlignment.DOUBLE_WORD -> 8
            }
        )

        bufferTextureData()
        GLUtils.checkErrorThrowing("Error while buffering texture data: $this")

        glPixelStorei(GL_UNPACK_ALIGNMENT, 4)

        val swizzleMask = when (descriptor.format) {
            TextureFormat.GRAY, TextureFormat.DEPTH, TextureFormat.STENCIL, TextureFormat.DEPTH_STENCIL
                -> intArrayOf(GL_RED, GL_RED, GL_RED, GL_ONE)

            TextureFormat.ALPHA -> intArrayOf(GL_ONE, GL_ONE, GL_ONE, GL_ALPHA, GL_RED)
            TextureFormat.GRAY_ALPHA -> intArrayOf(GL_RED, GL_RED, GL_RED, GL_GREEN)
            TextureFormat.RGB, TextureFormat.SRGB -> intArrayOf(GL_RED, GL_GREEN, GL_BLUE, GL_ONE)
            TextureFormat.RGBA, TextureFormat.SRGBA, TextureFormat.RGBA_HDR
                -> intArrayOf(GL_RED, GL_GREEN, GL_BLUE, GL_ALPHA)
        }

        glTexParameteriv(glTarget, GL_TEXTURE_SWIZZLE_RGBA, swizzleMask)

        if (descriptor.minFilter in setOf(TextureMinFilter.NEAREST, TextureMinFilter.LINEAR)
            && descriptor.format !in setOf(TextureFormat.DEPTH, TextureFormat.STENCIL, TextureFormat.DEPTH_STENCIL)
        ) {
            glGenerateMipmap(glTarget)
        }

        GLUtils.checkErrorThrowing("Error while creating texture: $this")
    }

    fun bind(unit: Int) = context.withContext {
        check(unit < context.maxTextureUnits) { "Texture unit $unit is not supported" }

        if (context.textureBindings[unit] != this) {
            glActiveTexture(GL_TEXTURE0 + unit)
            glBindTexture(glTarget, id)
            context.textureBindings[unit] = this
        }

        sync()
    }

    fun unbind(unit: Int) = context.withContext {
        if (context.textureBindings[unit] == this) {
            glActiveTexture(GL_TEXTURE0 + unit)
            glBindTexture(glTarget, 0)
            context.textureBindings[unit] = null
        }
    }

    private fun sync() {
        if (version >= descriptor.version) return

        context.withContext {
            glTexParameteri(glTarget, GL_TEXTURE_WRAP_S, descriptor.wrapping.gl)
            glTexParameteri(glTarget, GL_TEXTURE_WRAP_T, descriptor.wrapping.gl)
            glTexParameteri(glTarget, GL_TEXTURE_WRAP_R, descriptor.wrapping.gl)
        }

        version = descriptor.version
    }

    private var wasAlreadyDisposed = false

    actual override fun dispose() {
        if (wasAlreadyDisposed) return
        context.withContext { glDeleteTextures(id) }
        wasAlreadyDisposed = true
    }

    override fun toString(): String = "Texture(type=${descriptor.type}, format=${descriptor.format})"

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

internal actual fun loadTexture2DData(file: String, type: TextureType): TextureData {
    val width = createIntBuffer(1)
    val height = createIntBuffer(1)
    val numChannels = createIntBuffer(1)

    //stbi_set_flip_vertically_on_load(true) //uv coords are apparently already flipped while loading models?
    val textureData = stbi_load_from_memory(
        ResourceReader.getRawResource(file),
        width, height, numChannels, 0
    )
    if (textureData == null || !textureData.hasRemaining()) {
        throw MissingResourceException(
            "Texture $file could not be loaded:\n${stbi_failure_reason()}",
            Texture2D::class.simpleName, file
        )
    }

    val format = textureFormatFromNumChannelsAndType(numChannels.get(), type)
    val buffer = ByteArray(textureData.remaining()) { textureData[it] }

    stbi_image_free(textureData)

    return TextureData(Vector2i(width.get(), height.get()), format, buffer)
}
