package org.etieskrill.game.horde;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.service.Service;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

import static java.util.Comparator.comparingDouble;
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
                return new String[]{"BillBoard.glsl"};
            }

            @Override
            protected void getUniformLocations() {
                addUniform("camera", Uniform.Type.STRUCT);
                addUniform("position", Uniform.Type.VEC3);
                addUniform("billBoard", Uniform.Type.STRUCT);
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
    @SuppressWarnings({"rawtypes", "unchecked", "DataFlowIssue"})
    public @Nullable Comparator<Entity> comparator() {
        return (Comparator)
                comparingDouble(billBoard -> ((Entity) billBoard).getComponent(Transform.class).getPosition().z())
                        .reversed();
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        shader.start();

        shader.setUniform("camera", camera);
        shader.setUniform("position", targetEntity.getComponent(Transform.class).getPosition());

        shader.setUniform("billBoard", targetEntity.getComponent(BillBoard.class));

        glDisable(GL_CULL_FACE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glBindVertexArray(dummyVAO);
        glDrawArrays(GL_POINTS, 0, 1);
    }

}
