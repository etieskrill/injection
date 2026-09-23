package org.etieskrill.engine.graphics.texture

import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.texture.TextureMagFilter.LINEAR
import org.etieskrill.engine.graphics.texture.TextureMagFilter.NEAREST
import org.etieskrill.engine.graphics.texture.TextureMinFilter.*
import org.joml.Vector2ic
import org.joml.Vector4f
import org.joml.Vector4fc
import io.github.etieskrill.injection.extension.shader.Texture as DslTexture

abstract class Texture(
    val type: TextureType,
    val format: TextureFormat,
    val minFilter: TextureMinFilter = TRILINEAR,
    val magFilter: TextureMagFilter = LINEAR,
    wrapping: TextureWrapping = TextureWrapping.REPEAT,
    val borderColour: Vector4fc = Vector4f(0f),
    val rowAlignment: TextureRowAlignment = TextureRowAlignment.WORD
) : DslTexture {
    var wrapping: TextureWrapping = wrapping
        set(value) {
            version++
            field = value
        }

    internal var version: Long = 0L
}

/**
 * Broadly describes the purpose of a [Texture], and e.g. the exact [TextureFormat] may be derived from this when not
 * explicitly specified. This type itself may be inferred based on texture file name or type.
 *
 * Primarily used for rendering pipeline validation.
 */
enum class TextureType {
    UNKNOWN,

    DIFFUSE, SPECULAR, SHININESS, HEIGHT, EMISSIVE, NORMAL, METALNESS, ROUGHNESS, AMBIENT_OCCLUSION, //"regular" material textures

    SHADOW, //shadow maps

    G_POSITION, G_COLOUR, G_NORMAL, G_ORM, G_DEPTH //deferred rendering buffers
}

enum class TextureFormat(val numChannels: Int) {
    ALPHA(1),
    GRAY(1),
    GRAY_ALPHA(2),
    RGB(3),
    RGBA(4),
    RGBA_HDR(4),
    SRGB(3),
    SRGBA(4),
    DEPTH(1),
    STENCIL(1),
    DEPTH_STENCIL(2)
}

/**
 * The minification filtering mode describes the way textures are sampled if a fragment contains several texels.
 *
 * The general process differs from magnification due to the existence of mipmaps. There are now two parameters we
 * can decide on how to interpolate between values; both mipmaps and texels.
 *
 * The [NEAREST] and [LINEAR] modes ignore mipmapping entirely and will automatically disable the generation of mipmaps
 * when set, and work as their [TextureMagFilter] equivalents do.
 *
 * Other than those, the following combinations can be achieved:
 * - [NEAREST_NEAREST], which samples the closest texel from the closest mipmap. This is gives both sharp textures,
 *   and gives sharp edges when the mipmapping level changes.
 * - [BILINEAR], which interpolates between nearby texels sampled from the closest mipmap.
 *   Textures will look smooth, but the sharp edges on the mipmap borders remain.
 * - [NEAREST_LINEAR], which samples the closest texels from each of the surrounding mipmaps,
 *   and interpolates between those samples. Not used very often, unless fully sharp textures are an intentional look.
 * - [TRILINEAR], which interpolates the nearby texels sampled from the closest mipmaps,
 *   and interpolates between the mipmap levels. This is the standard option for most use cases.
 */

enum class TextureMinFilter { NEAREST, LINEAR, NEAREST_NEAREST, BILINEAR, NEAREST_LINEAR, TRILINEAR }
/**
 * The magnification filtering mode dictates how textures are sampled if one texel takes up more than one fragment.
 *
 * - [NEAREST] simply samples the closest texel, while
 * - [LINEAR] interpolates between the closest texels.
 *
 * [LINEAR] is recommended for most forms of textures, but [NEAREST] is useful for instance in pixelart textures.
 */
enum class TextureMagFilter { NEAREST, LINEAR }

enum class TextureWrapping { REPEAT, MIRRORED_REPEAT, CLAMP_TO_EDGE, CLAMP_TO_BORDER }

enum class TextureRowAlignment { BYTE, EVEN_BYTE, WORD, DOUBLE_WORD }

internal expect abstract class TextureInstance<T : Texture> : Disposable {
    val descriptor: T
    val context: GraphicsContext

    internal var version: Long

    override fun dispose()
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

internal class TextureData(val size: Vector2ic, val format: TextureFormat, val buffer: ByteArray)

internal expect fun loadTexture2DData(file: String, type: TextureType): TextureData
