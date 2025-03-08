package org.etieskrill.engine.entity.service.impl;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Drawable;
import org.etieskrill.engine.entity.component.PointLightComponent;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.service.Service;
import org.etieskrill.engine.graphics.Renderer;
import org.etieskrill.engine.graphics.animation.Animator;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer;
import org.etieskrill.engine.graphics.gl.shader.impl.DepthCubeMapArrayShader;
import org.etieskrill.engine.graphics.gl.shader.impl.DepthCubeMapArrayShaderKt;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.List;
import java.util.Objects;

public class PointShadowMappingService implements Service {

    private final Renderer renderer;
    private final DepthCubeMapArrayShader shader;

    private static final Matrix4fc DUMMY_MATRIX = new Matrix4f();

    private final int updateFrequency = 2;
    private int cycle = 0;

    public PointShadowMappingService(Renderer renderer, DepthCubeMapArrayShader shader) {
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

        DepthCubeMapArrayShaderKt.setLight(shader, component.getLight());
        DepthCubeMapArrayShaderKt.setIndex(shader, component.getShadowMapIndex() != null ? component.getShadowMapIndex() : 0);
        DepthCubeMapArrayShaderKt.setShadowCombined(shader, (Matrix4f[]) component.getCombinedMatrices());
        DepthCubeMapArrayShaderKt.setFarPlane(shader, component.getFarPlane() != null ? component.getFarPlane() : 0);

        component.getShadowMap().bind();
        for (Entity entity : entities) {
            if (entity.getId() == targetEntity.getId()) continue;

            Transform transform = entity.getComponent(Transform.class);
            Drawable drawable = entity.getComponent(Drawable.class);
            if (transform == null || drawable == null) continue;

            Animator animator = entity.getComponent(Animator.class);
            if (animator != null) {
                //FIXME actually use animated shader
                shader.setUniformArrayNonStrict("boneMatrices", animator.getTransformMatricesArray());
            }

            renderer.render(transform, drawable.getModel(), shader, DUMMY_MATRIX);
        }
        component.getShadowMap().unbind();
    }

}
