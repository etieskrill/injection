package org.etieskrill.engine.graphics.texture

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachmentInstance
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachmentType
import org.etieskrill.engine.graphics.framebuffer.FrameBufferInstance
import org.etieskrill.engine.graphics.framebuffer.gl
import org.etieskrill.engine.graphics.texture.TextureCubeMap.Companion.NUM_SIDES
import org.etieskrill.engine.util.ResourceReader
import org.etieskrill.engine.util.extension
import org.joml.Vector2ic
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL11C.glTexImage2D
import org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP
import org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP_POSITIVE_X
import org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL32C.glFramebufferTexture

private val logger = KotlinLogging.logger {}

internal actual class TextureCubeMapInstance(
    descriptor: TextureCubeMap,
    context: GraphicsContext
) : TextureInstance<TextureCubeMap>(descriptor, context), FrameBufferAttachmentInstance {

    override val glTarget: Int get() = GL_TEXTURE_CUBE_MAP

    override fun bufferTextureData() = context.withContext {
        val bytesExpectedPerSide = descriptor.run { size.x() * size.y() * format.numChannels }

        descriptor.buffer?.let { buffers ->
            check(buffers.size == NUM_SIDES) {
                "Cubemap texture data contained ${buffers.size} out of $NUM_SIDES buffers"
            }
            check(buffers.all { it.size == bytesExpectedPerSide }) {
                "Sides of cubemap texture data buffer contain [${
                    buffers.joinToString { it.size.toString() }
                }] bytes when $bytesExpectedPerSide bytes were expected per side"
            }
        }

        bind(0)

        when {
            descriptor.buffer != null -> {
                val buffer = BufferUtils.createByteBuffer(bytesExpectedPerSide)
                for (i in 0..<NUM_SIDES) {
                    glTexImage2D(
                        GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, descriptor.format.glInternal,
                        descriptor.size.x(), descriptor.size.y(), 0, descriptor.format.gl, GL_UNSIGNED_BYTE,
                        buffer.rewind().put(descriptor.buffer[i]).flip()
                    )
                }
            }

            descriptor.file != null -> {
                val (size, format, buffers) = readCubeMapFiles(descriptor.file, descriptor.type)
                check(descriptor.size == size && descriptor.format == format)
                val buffer = BufferUtils.createByteBuffer(bytesExpectedPerSide)
                for (i in 0..<NUM_SIDES) {
                    glTexImage2D(
                        GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, descriptor.format.glInternal,
                        descriptor.size.x(), descriptor.size.y(), 0, descriptor.format.gl, GL_UNSIGNED_BYTE,
                        buffer.rewind().put(buffers[i]).flip()
                    )
                }
            }
        }

        logger.debug {
            "Loaded ${descriptor.size.x()}x${descriptor.size.y()} ${8 * descriptor.format.numChannels}-bit ${
                descriptor.format.name.lowercase()
            } cubemap texture"
        }
    }

    override fun attach(frameBuffer: FrameBufferInstance, type: FrameBufferAttachmentType) {
        frameBuffer.bind()
        //This call binds the whole cubemap as a single shader object, where the faces are then
        //addressed using gl_Layer. The built-in variable does NOT work if we bound every face of the
        //cubemap using glFramebufferTexture2D, as the texture object's id would then refer to only the
        //last texture specified this way, which, when iterating over the faces, is the negative z one.
        glFramebufferTexture(GL_FRAMEBUFFER, type.gl, id, 0)

        //TODO is this true for texture arrays too? implement if so (must be for point shadow map arrays, right?)
    }

}

internal actual fun readCubeMapFiles(file: String, type: TextureType):
        Triple<Vector2ic, TextureFormat, List<ByteArray>> {
    check(ResourceReader.classpathResourceExists(file)) { "Cubemap directory $file not found" }

    val files = ResourceReader.getClasspathItems(file)
        .filter { it.extension == "png" || it.extension == "jpg" }

    check(files.size == NUM_SIDES) {
        "Cubemap directory $file contained ${files.size} instead of $NUM_SIDES textures"
    }

    val orderedFiles = Array<String?>(6) { null }
    fun Array<String?>.checkPut(index: Int, value: String) =
        if (get(index) == null) set(index, value)
        else error("Cubemap side name $value clashes with ${get(index)}")
    files.forEach {
        val file = it.lowercase()
        when {
            "right" in file || "px" in file -> orderedFiles.checkPut(0, it)
            "left" in file || "nx" in file -> orderedFiles.checkPut(1, it)
            "top" in file || "up" in file || "py" in file -> orderedFiles.checkPut(2, it)
            "bottom" in file || "down" in file || "ny" in file -> orderedFiles.checkPut(3, it)
            "front" in file || "pz" in file -> orderedFiles.checkPut(4, it)
            "back" in file || "nz" in file -> orderedFiles.checkPut(5, it)
            else -> error("Could not assign cubemap file $file to a side")
        }
    }
    check(orderedFiles.all { it != null }) {
        "Cubemap sides at indices [${
            orderedFiles.mapIndexedNotNull { i, file -> i.takeIf { file != null } }.joinToString()
        }] could not be derived based on file names"
    }

    var size: Vector2ic? = null
    var format: TextureFormat? = null
    val textureData = orderedFiles.map {
        loadTexture2DData(it!!, type).let { data ->
            if (size == null) size = data.size
            else check(data.size == size) { "Cubemap textures must all have the same size" }
            if (format == null) format = data.format
            else check(data.format == format) { "Cubemap textures must all have the same format" }
            data.buffer
        }
    }

    return Triple(size!!, format!!, textureData)
}
