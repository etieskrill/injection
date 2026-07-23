package org.etieskrill.engine.graphics.texture

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.util.ResourceReader
import org.etieskrill.engine.util.extension
import org.joml.Vector2ic
import org.joml.Vector4fc
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL11C.glTexImage2D
import org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP
import org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP_POSITIVE_X
import io.github.etieskrill.injection.extension.shader.TextureCubeMap as DslTextureCubeMap

private val logger = KotlinLogging.logger {}

actual class TextureCubeMap actual constructor(
    context: GraphicsContext,
    actual val size: Vector2ic,
    private var textureData: List<ByteArray>?,
    format: TextureFormat,
    type: TextureType,
    minFilter: TextureMinFilter,
    magFilter: TextureMagFilter,
    wrapping: TextureWrapping,
    borderColour: Vector4fc,
) : Texture(context, format, type, minFilter, magFilter, wrapping, borderColour), DslTextureCubeMap {

    override val glTarget: Int get() = GL_TEXTURE_CUBE_MAP

    actual companion object {
        actual val NUM_SIDES = 6

        actual fun createBlank(context: GraphicsContext, size: Vector2ic, format: TextureFormat) =
            TextureCubeMap(context, size, null, format = format)

        actual fun createFromBuffer(
            context: GraphicsContext, size: Vector2ic, buffer: List<ByteArray>, format: TextureFormat
        ) = TextureCubeMap(context, size, buffer, format = format)

        actual fun createFromFile(file: String, context: GraphicsContext, type: TextureType): TextureCubeMap {
            val (size, format, textureData) = readCubeMapFiles(file, type)
            return createFromBuffer(context, size, textureData, format)
        }

        fun createSkybox(file: String, context: GraphicsContext): TextureCubeMap {
            val (size, format, textureData) = readCubeMapFiles(file, TextureType.DIFFUSE)
            return TextureCubeMap(
                context, size, textureData, format, TextureType.DIFFUSE,
                TextureMinFilter.LINEAR, TextureMagFilter.LINEAR, TextureWrapping.CLAMP_TO_EDGE
            )
        }

        private fun readCubeMapFiles(file: String, type: TextureType):
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
    }

    override fun bufferTextureData() = context.withContext {
        val bytesExpectedPerSide = size.x() * size.y() * format.numChannels
        textureData?.let { textureData ->
            check(textureData.size == NUM_SIDES) {
                "Cubemap texture data contained ${textureData.size} out of $NUM_SIDES buffers"
            }
            check(textureData.all { it.size == bytesExpectedPerSide }) {
                "Sides of cubemap texture data buffer contain [${
                    textureData.joinToString { it.size.toString() }
                }] bytes when $bytesExpectedPerSide bytes were expected per side"
            }
        }

        bind(0)

        val buffer = BufferUtils.createByteBuffer(bytesExpectedPerSide)
        for (i in 0..<NUM_SIDES) {
            glTexImage2D(
                GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, format.glInternal,
                size.x(), size.y(), 0, format.gl, GL_UNSIGNED_BYTE,
                textureData?.get(i)?.let { buffer.rewind().put(it).flip() }
            )
        }
        textureData = null

        logger.debug {
            "Loaded ${size.x()}x${size.y()} ${8 * format.numChannels}-bit ${format.name.lowercase()} cubemap texture"
        }
    }

//    @Override
//    public void attach(FrameBufferAttachmentType type) {
//        //This call binds the whole cubemap as a single shader object, where the faces are then
//        //addressed using gl_Layer. The built-in variable does NOT work if we bound every face of the
//        //cubemap using glFramebufferTexture2D, as the texture object's id would then refer to only the
//        //last texture specified this way, which, when iterating over the faces, is the negative z one.
//        glFramebufferTexture(GL_FRAMEBUFFER, type.getGlAttachmentType(), getID(), 0);
//    }

}
