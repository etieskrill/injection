package org.etieskrill.game.horde.service;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.DirectionalLightComponent;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.service.Service;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.game.horde.component.AnimatedBillBoard;
import org.etieskrill.game.horde.component.BillBoard;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

import static java.util.Comparator.comparingDouble;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL30C.glGenVertexArrays;

public class BillBoardRenderService implements Service {

    private final ShaderProgram shader;
    private final ShaderProgram animatedShader;
    private final Camera camera;
    private final int dummyVAO;

    public BillBoardRenderService(Camera camera) {
        this.shader = new ShaderProgram() {
            @Override
            protected void init() {
                hasGeometryShader();
            }

            @Override
            protected String[] getShaderFileNames() {
                return new String[]{"BillBoard.glsl"};
            }

            @Override
            protected void getUniformLocations() {
                addUniform("camera", Uniform.Type.STRUCT);
                addUniform("position", Uniform.Type.VEC3);
                addUniform("dirLight", Uniform.Type.STRUCT);
                addUniform("dirLightCamera", Uniform.Type.STRUCT);
                addUniform("dirShadowMap", Uniform.Type.SAMPLER2D);
                addUniform("billBoard", Uniform.Type.STRUCT);
            }
        };
        this.animatedShader = new ShaderProgram() {
            @Override
            protected void init() {
                hasGeometryShader();
            }

            @Override
            protected String[] getShaderFileNames() {
                return new String[]{"AnimatedBillBoard.glsl"};
            }

            @Override
            protected void getUniformLocations() {
                addUniform("camera", Uniform.Type.STRUCT);
                addUniform("position", Uniform.Type.VEC3);
                addUniform("dirLight", Uniform.Type.STRUCT);
                addUniform("dirLightCamera", Uniform.Type.STRUCT);
                addUniform("dirShadowMap", Uniform.Type.SAMPLER2D);
                addUniform("animatedBillBoard", Uniform.Type.STRUCT);
            }
        };
        this.camera = camera;
        this.dummyVAO = glGenVertexArrays();
    }

    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(Transform.class, BillBoard.class)
                || entity.hasComponents(Transform.class, AnimatedBillBoard.class);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public @Nullable Comparator<Entity> comparator() {
        return (Comparator)
                comparingDouble(billBoard -> {
                    var transform = ((Entity) billBoard).getComponent(Transform.class);
                    return transform != null ? transform.getPosition().z() : 0; //FIXME comparator should never be served unprocessable entities, but that requires keeping the ordered lists in EntitySystem up to date, which will take some effort
                }).reversed();
    }

    @Override
    public void preProcess(List<Entity> entities) {
        FrameBuffer.bindScreenBuffer();
        glViewport(0, 0, camera.getViewportSize().x(), camera.getViewportSize().y());

        configureShader(entities, shader);
        configureShader(entities, animatedShader);
    }

    //TODO eww
    private void configureShader(List<Entity> entities, ShaderProgram shader) {
        shader.start();
        for (Entity entity : entities) {
            var dirLight = entity.getComponent(DirectionalLightComponent.class);
            if (dirLight != null) {
                shader.setUniform("dirLightCamera", dirLight.getCamera());
                shader.setUniform("dirLight", dirLight.getDirectionalLight());
                shader.setTexture("dirShadowMap", dirLight.getShadowMap().getTexture());
                break; //the sun will probs be the only dir light, and interiors will use a different shader or something
            }
        }

        shader.setUniform("camera", camera);
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        var billBoard = targetEntity.getComponent(BillBoard.class);
        if (billBoard != null) {
            shader.setUniform("position", targetEntity.getComponent(Transform.class).getPosition());
            shader.setUniform("billBoard", billBoard);
        } else {
            animatedShader.setUniform("position", targetEntity.getComponent(Transform.class).getPosition());
            animatedShader.setUniform("animatedBillBoard", targetEntity.getComponent(AnimatedBillBoard.class));
        }

        glDisable(GL_CULL_FACE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glBindVertexArray(dummyVAO);
        glDrawArrays(GL_POINTS, 0, 1);
    }

}
