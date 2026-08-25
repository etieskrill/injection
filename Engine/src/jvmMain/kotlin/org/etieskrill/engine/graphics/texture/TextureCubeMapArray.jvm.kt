package org.etieskrill.engine.graphics.texture

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.graphics.GraphicsContext
import org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL12C.glTexImage3D
import org.lwjgl.opengl.GL40C.GL_TEXTURE_CUBE_MAP_ARRAY
import java.nio.ByteBuffer

private val logger = KotlinLogging.logger {}

internal actual class TextureCubeMapArrayInstance(
    descriptor: TextureCubeMapArray,
    context: GraphicsContext
) : TextureInstance<TextureCubeMapArray>(descriptor, context) {

    override val glTarget: Int get() = GL_TEXTURE_CUBE_MAP_ARRAY

    override fun bufferTextureData() = context.withContext {
        bind(0)

        @Suppress("USELESS_CAST") val data = when {
            descriptor.buffer != null -> TODO()
            descriptor.file != null -> TODO()
            else -> null as? ByteBuffer
        }

        glTexImage3D(
            glTarget, 0, descriptor.format.glInternal,
            descriptor.size.x(), descriptor.size.y(), TextureCubeMap.NUM_SIDES * descriptor.length,
            0, descriptor.format.gl, GL_UNSIGNED_BYTE, data
        )

        logger.debug {
            "Loaded ${descriptor.size.x()}x${descriptor.size.y()}x${descriptor.length} ${
                descriptor.format.numChannels
            }-bit ${descriptor.format.name.lowercase()} ${descriptor.type.name.lowercase()} cubemap array texture"
        }
    }

//    @Override
//    public void attach(FrameBufferAttachmentType type) {
//        glFramebufferTexture(GL_FRAMEBUFFER, type.getGlAttachmentType(), getID(), 0);
//    }

}
