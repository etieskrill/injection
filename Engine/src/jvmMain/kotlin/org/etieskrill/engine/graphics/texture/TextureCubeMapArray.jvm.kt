package org.etieskrill.engine.graphics.texture;

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.graphics.GraphicsContext
import org.joml.Vector2ic
import org.joml.Vector4fc
import org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL12C.glTexImage3D
import org.lwjgl.opengl.GL40C.GL_TEXTURE_CUBE_MAP_ARRAY
import java.nio.ByteBuffer
import io.github.etieskrill.injection.extension.shader.TextureCubeMapArray as DslTextureCubeMapArray

private val logger = KotlinLogging.logger {}

actual class TextureCubeMapArray actual constructor(
    context: GraphicsContext,
    actual val size: Vector2ic,
    actual val length: Int,
    format: TextureFormat,
    type: TextureType,
    minFilter: TextureMinFilter,
    magFilter: TextureMagFilter,
    wrapping: TextureWrapping,
    borderColour: Vector4fc,
) : Texture(context, format, type, minFilter, magFilter, wrapping, borderColour), DslTextureCubeMapArray {

    override val glTarget: Int get() = GL_TEXTURE_CUBE_MAP_ARRAY

    override fun bufferTextureData() = context.withContext {
        bind(0)

        @Suppress("USELESS_CAST")
        glTexImage3D(
            glTarget, 0, format.glInternal,
            size.x(), size.y(), TextureCubeMap.NUM_SIDES * length,
            0, format.gl, GL_UNSIGNED_BYTE, null as? ByteBuffer
        )

        logger.debug {
            "Loaded ${size.x()}x${size.y()}x${length} ${format.numChannels}-bit ${format.name.lowercase()}" +
                    " ${type.name.lowercase()} cubemap array texture"
        }
    }

//    @Override
//    public void attach(FrameBufferAttachmentType type) {
//        glFramebufferTexture(GL_FRAMEBUFFER, type.getGlAttachmentType(), getID(), 0);
//    }

}
