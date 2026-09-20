package org.etieskrill.engine.graphics

import org.etieskrill.engine.graphics.buffer.BufferObject
import org.etieskrill.engine.graphics.buffer.BufferObjectInstance
import org.etieskrill.engine.graphics.buffer.BufferType
import org.etieskrill.engine.graphics.buffer.StorageBufferObject
import org.etieskrill.engine.graphics.buffer.StorageBufferObjectInstance
import org.etieskrill.engine.graphics.buffer.VertexArrayObject
import org.etieskrill.engine.graphics.buffer.VertexArrayObjectInstance
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachment
import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachmentInstance
import org.etieskrill.engine.graphics.framebuffer.FrameBufferInstance
import org.etieskrill.engine.graphics.framebuffer.RenderBuffer
import org.etieskrill.engine.graphics.framebuffer.RenderBufferInstance
import org.etieskrill.engine.graphics.framebuffer.ScreenBuffer
import org.etieskrill.engine.graphics.pipeline.AlphaMode
import org.etieskrill.engine.graphics.pipeline.CullingMode
import org.etieskrill.engine.graphics.pipeline.FillMode
import org.etieskrill.engine.graphics.pipeline.StencilMode
import org.etieskrill.engine.graphics.shader.Shader
import org.etieskrill.engine.graphics.shader.ShaderInstance
import org.etieskrill.engine.graphics.texture.ArrayTexture2D
import org.etieskrill.engine.graphics.texture.ArrayTexture2DInstance
import org.etieskrill.engine.graphics.texture.Texture
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.graphics.texture.Texture2DInstance
import org.etieskrill.engine.graphics.texture.TextureCubeMap
import org.etieskrill.engine.graphics.texture.TextureCubeMapArray
import org.etieskrill.engine.graphics.texture.TextureCubeMapArrayInstance
import org.etieskrill.engine.graphics.texture.TextureCubeMapInstance
import org.etieskrill.engine.graphics.texture.TextureInstance
import org.lwjgl.opengl.GL11C.GL_ALWAYS
import org.lwjgl.opengl.GL11C.GL_BACK
import org.lwjgl.opengl.GL11C.GL_BLEND
import org.lwjgl.opengl.GL11C.GL_CULL_FACE
import org.lwjgl.opengl.GL11C.GL_DEPTH_TEST
import org.lwjgl.opengl.GL11C.GL_EQUAL
import org.lwjgl.opengl.GL11C.GL_FILL
import org.lwjgl.opengl.GL11C.GL_FRONT
import org.lwjgl.opengl.GL11C.GL_FRONT_AND_BACK
import org.lwjgl.opengl.GL11C.GL_KEEP
import org.lwjgl.opengl.GL11C.GL_LINE
import org.lwjgl.opengl.GL11C.GL_LINE_SMOOTH
import org.lwjgl.opengl.GL11C.GL_NOTEQUAL
import org.lwjgl.opengl.GL11C.GL_ONE
import org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA
import org.lwjgl.opengl.GL11C.GL_POINT
import org.lwjgl.opengl.GL11C.GL_REPLACE
import org.lwjgl.opengl.GL11C.GL_SRC_ALPHA
import org.lwjgl.opengl.GL11C.GL_STENCIL_TEST
import org.lwjgl.opengl.GL11C.GL_ZERO
import org.lwjgl.opengl.GL11C.glBlendFunc
import org.lwjgl.opengl.GL11C.glCullFace
import org.lwjgl.opengl.GL11C.glDepthMask
import org.lwjgl.opengl.GL11C.glDisable
import org.lwjgl.opengl.GL11C.glEnable
import org.lwjgl.opengl.GL11C.glLineWidth
import org.lwjgl.opengl.GL11C.glPointSize
import org.lwjgl.opengl.GL11C.glPolygonMode
import org.lwjgl.opengl.GL11C.glStencilFunc
import org.lwjgl.opengl.GL11C.glStencilMask
import org.lwjgl.opengl.GL11C.glStencilOp
import kotlin.properties.Delegates.notNull

//TODO put all withContext-esque wrappers here, including a wrapper for error functions
//we guarantee the construction of an instance will happen while the respective context is active, but any calls outside
//of that will still have to manually use a withContext function
actual data class GraphicsContext(
    val maxTextureUnits: Int,
    val maxVertexAttributes: Int
) {

    internal var thread: Thread? = null

    var activeFramebuffer: FrameBufferInstance by notNull(); internal set
    actual var screenBuffer: FrameBufferInstance by notNull(); internal set
    private val frameBuffers = mutableMapOf<FrameBuffer, FrameBufferInstance>()
    internal fun getFrameBuffer(frameBuffer: FrameBuffer) =
        if (frameBuffer is ScreenBuffer) screenBuffer
        else frameBuffers.getOrPut(frameBuffer) { FrameBufferInstance(frameBuffer, this) }

    private val textures = mutableMapOf<Texture, TextureInstance<*>>()

    @Suppress("UNCHECKED_CAST")
    internal fun <T : Texture> getTexture(texture: T): TextureInstance<T> = textures.getOrPut(texture) {
        when (texture) {
            is Texture2D -> Texture2DInstance(texture, this)
            is ArrayTexture2D -> ArrayTexture2DInstance(texture, this)
            else -> error("Unknown texture type ${texture::class.simpleName}")
        }
    } as TextureInstance<T>

    //TODO other texture types
    internal val textureBindings = Array<TextureInstance<*>?>(maxTextureUnits) { null }

    internal var activeTexture: Int = 0

    private val bufferObjects = mutableMapOf<BufferObject<*>, BufferObjectInstance<*>>()
    internal fun getBufferObject(buffer: BufferObject<*>) =
        bufferObjects.getOrPut(buffer) { BufferObjectInstance(buffer, this) }

    internal val bufferBindings = mutableMapOf<BufferType, BufferObjectInstance<*>>()

    private val storageBufferObjects = mutableMapOf<StorageBufferObject<*>, StorageBufferObjectInstance<*>>()
    internal fun getStorageBufferObject(buffer: StorageBufferObject<*>) =
        storageBufferObjects.getOrPut(buffer) { StorageBufferObjectInstance(buffer, this) }

    internal val storageBufferBindings = mutableMapOf<Int, StorageBufferObjectInstance<*>>()

    private val renderBuffers = mutableMapOf<RenderBuffer, RenderBufferInstance>()
    internal fun getRenderBuffer(renderBuffer: RenderBuffer) =
        renderBuffers.getOrPut(renderBuffer) { RenderBufferInstance(renderBuffer, this) }

    private val shaders = mutableMapOf<Shader, ShaderInstance>()
    internal fun getShader(shader: Shader) =
        shaders.getOrPut(shader) { ShaderInstance(shader, this) }

    internal var activeShader: ShaderInstance? = null

    private val vertexArrays = mutableMapOf<VertexArrayObject<*>, VertexArrayObjectInstance<*>>()
    internal fun <T> getVertexArray(vertexArray: VertexArrayObject<T>) =
        vertexArrays.getOrPut(vertexArray) { VertexArrayObjectInstance(vertexArray, this) }

    internal var activeVertexArray: VertexArrayObjectInstance<*>? = null //TODO add dummy vao (or the proper solution, if there is one)

    internal var alphaMode: AlphaMode = AlphaMode.OPAQUE
        set(value) {
            if (field == value) return
            glEnable(GL_BLEND)
            when (value) {
                AlphaMode.OPAQUE -> glBlendFunc(GL_ONE, GL_ZERO)
                AlphaMode.SOURCE_ALPHA -> glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            }
            field = value
        }

    internal var cullingMode: CullingMode = CullingMode.BACK
        set(value) {
            if (field == value) return
            when (value) {
                CullingMode.NONE -> glDisable(GL_CULL_FACE)
                CullingMode.BACK -> {
                    glEnable(GL_CULL_FACE)
                    glCullFace(GL_BACK)
                }
                CullingMode.FRONT -> {
                    glEnable(GL_CULL_FACE)
                    glCullFace(GL_FRONT)
                }
                CullingMode.FRONT_AND_BACK -> {
                    glEnable(GL_CULL_FACE)
                    glCullFace(GL_FRONT_AND_BACK)
                }
            }
        }

    internal var depthTest: Boolean = true
        set(value) {
            if (field == value) return
            if (value) {
                glEnable(GL_DEPTH_TEST)
            } else {
                glDisable(GL_DEPTH_TEST)
            }
            field = value
        }

    internal var depthWrite: Boolean = true
        set(value) {
            if (field == value) return
            glDepthMask(value)
            field = value
        }

    internal var fillMode: FillMode = FillMode.FILL
        set(value) {
            if (field == value) return
            glPolygonMode(
            GL_FRONT_AND_BACK, when (value) {
                FillMode.FILL -> GL_FILL
                FillMode.LINE -> GL_LINE
                FillMode.POINT -> GL_POINT
            })
            field = value
        }

    internal var pointSize: Float = 1f
        set(value) {
            if (field == value) return
            glPointSize(value)
            field = value
        }
    internal var lineWidth: Float = 1f
        set(value) {
            if (field == value) return
            glLineWidth(value)
            field = value
        }
    internal var lineAntiAliasing: Boolean = true
        set(value) {
            if (field == value) return
            if (value) {
                glEnable(GL_LINE_SMOOTH)
            } else {
                glDisable(GL_LINE_SMOOTH)
            }
            field = value
        }

    internal var stencilMode: StencilMode = StencilMode.OFF
        set(value) {
            if (field == value) return
            when (value) {
                StencilMode.OFF -> {
                    glDisable(GL_STENCIL_TEST)
                    glStencilMask(0x00)
                }
                StencilMode.SET_FRONT -> {
                    glEnable(GL_STENCIL_TEST)
                    glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
                    glStencilFunc(GL_ALWAYS, 0xFF, 0xFF)
                    glStencilMask(0xFF)
                }
                StencilMode.FILTER -> {
                    glEnable(GL_STENCIL_TEST)
                    glStencilFunc(GL_EQUAL, 0xFF, 0xFF)
                    glStencilMask(0x00)
                }
                StencilMode.FILTER_NOT -> {
                    glEnable(GL_STENCIL_TEST)
                    glStencilFunc(GL_NOTEQUAL, 0xFF, 0xFF)
                    glStencilMask(0x00)
                }
            }
            field = value
        }

    init {
        alphaMode = AlphaMode.OPAQUE
        cullingMode = CullingMode.BACK
        depthTest = true
        depthWrite = true
        fillMode = FillMode.FILL
        pointSize = 1f
        lineWidth = 1f
        lineAntiAliasing = true
        stencilMode = StencilMode.OFF
    }

    internal fun getFrameBufferAttachment(frameBufferAttachment: FrameBufferAttachment): FrameBufferAttachmentInstance =
        when (frameBufferAttachment) {
            is RenderBuffer -> getRenderBuffer(frameBufferAttachment)
            is Texture2D -> getTexture(frameBufferAttachment) as Texture2DInstance
            is TextureCubeMap -> getTexture(frameBufferAttachment) as TextureCubeMapInstance
            is TextureCubeMapArray -> getTexture(frameBufferAttachment) as TextureCubeMapArrayInstance
            else -> error("Dipshit dev forgot to add framebuffer attachment: ${frameBufferAttachment::class.simpleName}")
        }

    actual fun <T> withContext(block: () -> T): T {
        checkThread() //TODO use rendering coroutine withContext instead
        return block()
    }

    companion object {
        val CONTEXT: ThreadLocal<GraphicsContext> = ThreadLocal<GraphicsContext>.withInitial {
            error("Tried to access graphics context in non-current thread")
        }

        @get:JvmName("MAX_TEXTURE_UNITS")
        val MAX_TEXTURE_UNITS get() = CONTEXT.get().maxTextureUnits

        @get:JvmName("MAX_VERTEX_ATTRIBUTES")
        val MAX_VERTEX_ATTRIBUTES get() = CONTEXT.get().maxVertexAttributes
    }

    internal fun checkThread() =
        check(Thread.currentThread() == thread) {
            "This method may only be called from the thread the graphics context is attached to"
        }

}
