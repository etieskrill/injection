package org.etieskrill.engine.entity.service.impl;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Drawable;
import org.etieskrill.engine.entity.component.PointLightComponent;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.component.TransformC;
import org.etieskrill.engine.entity.service.Service;
import org.etieskrill.engine.graphics.Renderer;
import org.etieskrill.engine.graphics.animation.Animator;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer;
import org.etieskrill.engine.graphics.gl.shader.Shaders;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.List;
import java.util.Objects;

public class PointShadowMappingService implements Service {

    private final Renderer renderer;
    private final Shaders.DepthCubeMapArrayShader shader;

    private static final Matrix4fc DUMMY_MATRIX = new Matrix4f();

    private final int updateFrequency = 2;
    private int cycle = 0;

    public PointShadowMappingService(Renderer renderer, Shaders.DepthCubeMapArrayShader shader) {
        this.renderer = renderer;
        this.shader = shader;
    }

    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(PointLightComponent.class);
    }

    @Override
    public void preProcess(List<Entity> entities) {
        if (++cycle >= updateFrequency) {
            cycle = 0;
        } else return;

        entities.stream()
                .map(entity -> entity.getComponent(PointLightComponent.class))
                .filter(Objects::nonNull)
                .map(PointLightComponent::getShadowMap)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(FrameBuffer::clear);
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        if (cycle != 0) return;

        PointLightComponent component = targetEntity.getComponent(PointLightComponent.class);
        if (component.getShadowMap() == null) return; //TODO resolve by using separate component for shadow maps

        shader.setLight(component.getLight());
        shader.setIndex(component.getShadowMapIndex());
        shader.setShadowCombined(component.getCombinedMatrices());
        shader.setFarPlane(component.getFarPlane());

        component.getShadowMap().bind();
        for (Entity entity : entities) {
            if (entity.getId() == targetEntity.getId()) continue;

            Transform transform = entity.getComponent(Transform.class);
            Drawable drawable = entity.getComponent(Drawable.class);
            if (transform == null || drawable == null) continue;

            Animator animator = entity.getComponent(Animator.class);
            if (animator != null) {
                shader.setUniformArrayNonStrict("boneMatrices", animator.getTransformMatricesArray());
            }

            TransformC finalTransform = new Transform(transform).compose(drawable.getModel().getInitialTransform());
            renderer.render(finalTransform, drawable.getModel(), shader, DUMMY_MATRIX);
        }
        component.getShadowMap().unbind();
    }

}
