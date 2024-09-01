package org.etieskrill.engine.graphics.gl.renderer;

import lombok.extern.slf4j.Slf4j;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.gl.VertexArrayObject;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.impl.ParticleShader;
import org.etieskrill.engine.graphics.particle.*;
import org.etieskrill.engine.util.Loaders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNullElse;
import static org.lwjgl.opengl.GL11C.*;

@Slf4j
public class GLParticleRenderer implements ParticleRenderer {

    public static final int MAX_PARTICLES = 10_000;

    private static final Matrix4fc IDENTITY = new Matrix4f();

    private final VertexArrayObject<Particle> vao;
    private final ShaderProgram particleShader;

    private final Set<ParticleEmitter> invalidEmitters;

    //TODO render stats
    public GLParticleRenderer() {
        this.vao = VertexArrayObject
                .builder(ParticleVertexAccessor.getInstance())
                .numVertexElements((long) MAX_PARTICLES)
                .build();
        this.particleShader = Loaders.ShaderLoader.get()
                .load("particle_shader", ParticleShader::new);

        this.invalidEmitters = new HashSet<>();
    }

    @Override
    public void renderParticles(ParticleNode root, Camera camera, @Nullable ShaderProgram shader) {
        renderNode(new Matrix4f(root.getTransform().getMatrix()), root, camera, requireNonNullElse(shader, particleShader));
    }

    private void renderNode(Matrix4fc transform, ParticleNode node, Camera camera, ShaderProgram shader) {
        for (@NotNull ParticleEmitter emitter : node.getEmitters()) {
            if (!invalidEmitters.contains(emitter)) {
                if (emitter.getMaxNumParticles() <= MAX_PARTICLES) {
                    renderEmitter(new Matrix4f(transform).mul(emitter.getTransform().getMatrix()), emitter, camera, shader);
                } else {
                    invalidEmitters.add(emitter);
                    logger.warn("Emitter has max of {} particles, but renderer can only draw {}", emitter.getMaxNumParticles(), MAX_PARTICLES);
                }
            }
        }

        for (@NotNull ParticleNode child : node.getChildren()) {
            Matrix4f childTransform = new Matrix4f(transform).mul(child.getTransform().getMatrix());
            renderNode(childTransform, child, camera, shader);
        }
    }

    private void renderEmitter(Matrix4fc transform, ParticleEmitter emitter, Camera camera, ShaderProgram shader) {
        shader.setUniform("model", emitter.isParticlesMoveWithEmitter() ? transform : IDENTITY);
        shader.setUniform("camera", camera);
        shader.setUniform("size", emitter.getSize());
        emitter.getSprite().bind(0);
        shader.setUniform("sprite", 0);

        vao.setVertices(emitter.getAliveParticles());
        vao.bind();

        glDisable(GL_CULL_FACE);
        glDepthMask(false);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDrawArrays(GL_POINTS, 0, emitter.getAliveParticles().size());
        glBlendFunc(GL_ONE, GL_ZERO);
        glDepthMask(true);
        glEnable(GL_CULL_FACE);

        VertexArrayObject.unbind();
    }

    @Override
    public void dispose() {
        vao.dispose();
    }

}
