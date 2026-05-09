package org.etieskrill.engine.graphics.gl.framebuffer

import io.github.etieskrill.injection.extension.shader.TextureShadow
import org.etieskrill.engine.graphics.texture.AbstractTexture
import org.joml.Vector2ic
import org.lwjgl.opengl.GL11C.GL_LEQUAL
import org.lwjgl.opengl.GL11C.glTexParameteri
import org.lwjgl.opengl.GL14C.GL_TEXTURE_COMPARE_FUNC
import org.lwjgl.opengl.GL14C.GL_TEXTURE_COMPARE_MODE
import org.lwjgl.opengl.GL30C.GL_COMPARE_REF_TO_TEXTURE

/**
 * It is possible to use a regular [texture][AbstractTexture] (with a `sampler{*D,Cube}}`) as a shadow map, but this
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
        where T : AbstractTexture, T : FrameBufferAttachment {

    init {
        texture.bind()
        glTexParameteri(texture.target.gl(), GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE)
        glTexParameteri(texture.target.gl(), GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL)
    }

    override fun bind() = super<FrameBuffer>.bind()

    override fun bind(unit: Int) = texture.bind(unit)
    override fun unbind(unit: Int) = texture.unbind(unit)

}
