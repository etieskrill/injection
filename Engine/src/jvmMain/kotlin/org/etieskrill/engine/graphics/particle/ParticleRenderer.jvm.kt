package org.etieskrill.engine.graphics.particle

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.buffer.BufferAccessFrequency
import org.etieskrill.engine.graphics.buffer.BufferObject
import org.etieskrill.engine.graphics.buffer.VertexArrayObject
import org.etieskrill.engine.graphics.camera.Camera
import org.etieskrill.engine.graphics.shader.Shader
import org.etieskrill.engine.graphics.shader.impl.ParticleShader
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.graphics.texture.TextureType
import org.etieskrill.engine.util.EngineShaderLoader
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.times
import org.lwjgl.opengl.GL11C.*

private val logger = KotlinLogging.logger {}

actual class ParticleRenderer actual constructor(
    actual val context: GraphicsContext
) {

    private val vao = VertexArrayObject(
        ParticleVertexAccessor,
        BufferObject(ParticleVertexAccessor, MAX_PARTICLES, accessFrequency = BufferAccessFrequency.STREAM),
        null
    )

    private val particleShader = EngineShaderLoader.load("particle_shader") { ParticleShader() }

    val defaultParticleTexture =
        Texture2D.createFromFile("textures/particles/circle.png", TextureType.DIFFUSE) //TODO wrapping = CLAMP_TO_BORDER

    private val invalidEmitters = mutableSetOf<ParticleEmitter>()

    companion object {
        const val MAX_PARTICLES = 10_000

        private val IDENTITY = Matrix4f()
    }

    actual fun renderParticles(root: ParticleNode, camera: Camera, shader: Shader?) {
        renderNode(Matrix4f(root.transform.matrix), root, camera, shader ?: particleShader)
    }

    private fun renderNode(transform: Matrix4fc, node: ParticleNode, camera: Camera, shader: Shader) {
        node.emitters.forEach { emitter ->
            if (emitter !in invalidEmitters) {
                if (emitter.maxNumParticles <= MAX_PARTICLES) {
                    renderEmitter(transform * emitter.transform.matrix, emitter, camera, shader)
                } else {
                    invalidEmitters += emitter
                    logger.warn { "Emitter has max of ${emitter.maxNumParticles} particles, but renderer can only draw $MAX_PARTICLES" }
                }
            }
        }

        node.children.forEach { child ->
            renderNode(transform * child.transform.matrix, child, camera, shader)
        }
    }

    private fun renderEmitter(transform: Matrix4fc, emitter: ParticleEmitter, camera: Camera, shader: Shader) {
        shader.setUniform("model", if (emitter.particlesMoveWithEmitter) transform else IDENTITY)
        shader.setUniform("camera", camera)
        shader.setUniform("size", emitter.size)
        shader.setTexture("sprite", emitter.sprite ?: defaultParticleTexture)

        vao.vertices = emitter.aliveParticles

        context.withContext {
            val vaoInstance = context.getVertexArray(vao)
            vaoInstance.bind()

            glDisable(GL_CULL_FACE)
            glDepthMask(false)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glDrawArrays(GL_POINTS, 0, emitter.aliveParticles.size)
            glBlendFunc(GL_ONE, GL_ZERO)
            glDepthMask(true)
            glEnable(GL_CULL_FACE)

            vaoInstance.unbind()
        }
    }

}
