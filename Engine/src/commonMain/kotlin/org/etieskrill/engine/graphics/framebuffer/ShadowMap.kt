package org.etieskrill.engine.graphics.framebuffer

import io.github.etieskrill.injection.extension.shader.TextureShadow
import org.etieskrill.engine.graphics.texture.Texture
import org.joml.Vector2ic

/**
 * It is possible to use a regular [texture][Texture] (with a `sampler{*D,Cube}}`) as a shadow map, but this
 * introduces unnecessary wrangling with colour vectors, among other inconveniences. Instead, leverage a shadow sampler
 * (`sampler{*D,Cube}Shadow`) by using this class.
 *
 * Note that; while it is possible on various hardware to read from a shadow/depth texture using a non-shadow sampler
 * and vice versa, and this class effectively only acts as a proxy for the [ShadowMap.texture], this action causes
 * undefined behaviour according to the specification.
 */
abstract class ShadowMap<T>(
    size: Vector2ic,
    val texture: T
) : FrameBuffer(size, mapOf(FrameBufferAttachmentType.DEPTH to texture)), TextureShadow
        where T : Texture, T : FrameBufferAttachment

internal expect abstract class ShadowMapInstance<T> :
    FrameBufferInstance, FrameBufferAttachmentInstance where T : Texture, T : FrameBufferAttachment {
    override val descriptor: ShadowMap<T>
}
