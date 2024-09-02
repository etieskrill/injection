package org.etieskrill.game.horde;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.service.Service;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

import java.util.List;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL30C.glGenVertexArrays;

public class BillBoardRenderService implements Service {

    private final ShaderProgram shader;
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
                return new String[]{"Blit.glsl"};
            }

            @Override
            protected void getUniformLocations() {
                addUniform("camera", Uniform.Type.STRUCT);
                addUniform("position", Uniform.Type.VEC3);
                addUniform("offset", Uniform.Type.VEC3);
                addUniform("size", Uniform.Type.VEC2);
                addUniform("diffuse", Uniform.Type.SAMPLER2D);
            }
        };
        ;
        this.camera = camera;
        this.dummyVAO = glGenVertexArrays();
    }

    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(Transform.class, BillBoard.class);
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        shader.setUniform("camera", camera);
        shader.setUniform("position", targetEntity.getComponent(Transform.class).getPosition());

        BillBoard billBoard = targetEntity.getComponent(BillBoard.class);
        shader.setUniform("diffuse", 0);
        billBoard.getSprite().bind(0);
        shader.setUniform("size", billBoard.getSize());
        shader.setUniform("offset", billBoard.getOffset());

        shader.start();

        glDisable(GL_CULL_FACE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glBindVertexArray(dummyVAO);
        glDrawArrays(GL_POINTS, 0, 1);
    }

}
