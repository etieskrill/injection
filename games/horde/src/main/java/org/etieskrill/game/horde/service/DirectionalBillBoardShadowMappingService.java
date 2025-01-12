package org.etieskrill.game.horde.service;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.DirectionalLightComponent;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.service.Service;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.Shaders;
import org.etieskrill.game.horde.component.AnimatedBillBoard;
import org.etieskrill.game.horde.component.BillBoard;
import org.joml.Matrix3f;

import java.util.ArrayList;
import java.util.List;

import static java.util.Comparator.comparingDouble;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL45C.glCreateVertexArrays;

public class DirectionalBillBoardShadowMappingService implements Service {

    private final Camera billBoardCamera;
    private final ShaderProgram billboardDepthShader;
    private final ShaderProgram animatedBillboardDepthShader;

    private final int dummyVao;
    private final List<Entity> orderedEntities = new ArrayList<>();

    public DirectionalBillBoardShadowMappingService(Camera billBoardCamera) {
        this.billBoardCamera = billBoardCamera;
        this.billboardDepthShader = new ShaderProgram(List.of("DepthBillBoard.glsl"), false) {
        };
        this.animatedBillboardDepthShader = new ShaderProgram(List.of("AnimatedDepthBillBoard.glsl"), false) {
        };
        this.dummyVao = glCreateVertexArrays();
    }

    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(DirectionalLightComponent.class);
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        DirectionalLightComponent shadowMapComponent = targetEntity.getComponent(DirectionalLightComponent.class);

        shadowMapComponent.getShadowMap().clear();
        shadowMapComponent.getShadowMap().bind();

        glBindVertexArray(dummyVao);

        orderedEntities.clear();
        orderedEntities.addAll(entities);
        orderedEntities.sort(comparingDouble(entity -> {
            var transform = entity.getComponent(Transform.class);
            return transform != null ? transform.getPosition().z() : Double.MAX_VALUE;
        }));

        billboardDepthShader.setUniform("camera", shadowMapComponent.getCamera());
        billboardDepthShader.setUniform("cameraRotation", billBoardCamera.getRotation().get(new Matrix3f()));

        animatedBillboardDepthShader.setUniform("camera", shadowMapComponent.getCamera());
        animatedBillboardDepthShader.setUniform("cameraRotation", billBoardCamera.getRotation().get(new Matrix3f()));

        for (Entity entity : entities) {
            Transform transform = entity.getComponent(Transform.class);
            BillBoard billBoard = entity.getComponent(BillBoard.class);
            AnimatedBillBoard animatedBillBoard = entity.getComponent(AnimatedBillBoard.class);
            if (transform == null) continue;

            if (billBoard != null) {
                billboardDepthShader.setUniform("position", transform.getPosition());
                billboardDepthShader.setUniform("billBoard", billBoard);
                billboardDepthShader.start();
            } else if (animatedBillBoard != null) {
                animatedBillboardDepthShader.setUniform("position", transform.getPosition());
                animatedBillboardDepthShader.setUniform("animatedBillBoard", animatedBillBoard);
                animatedBillboardDepthShader.start();
            } else {
                return;
            }

            glEnable(GL_DEPTH_TEST);
            glDrawArrays(GL_POINTS, 0, 1);
        }

        shadowMapComponent.getShadowMap().unbind();
    }

}
