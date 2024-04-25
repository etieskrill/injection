package org.etieskrill.engine.entity.service;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Drawable;
import org.etieskrill.engine.entity.component.PointLightComponent;
import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.entity.data.TransformC;
import org.etieskrill.engine.graphics.Renderer;
import org.etieskrill.engine.graphics.gl.shader.Shaders;

import java.util.List;
import java.util.Objects;

import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.glClear;

public class PointShadowMappingService implements Service {

    private final Renderer renderer;
    private final Shaders.DepthCubeMapArrayShader shader;

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
        entities.stream()
                .map(entity -> entity.getComponent(PointLightComponent.class))
                .filter(Objects::nonNull)
                .map(PointLightComponent::getShadowMap)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(shadowMap -> {
                    shadowMap.bind();
                    glClear(GL_DEPTH_BUFFER_BIT);
                    shadowMap.unbind();
                });
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        PointLightComponent component = targetEntity.getComponent(PointLightComponent.class);
        if (component.getShadowMap() == null) return; //TODO resolve by using separate component for shadow maps

        shader.setLight(component.getLight());
        shader.setIndex(component.getShadowMapIndex());
        shader.setShadowCombined(component.getCombinedMatrices());
        shader.setFarPlane(component.getFarPlane());

        component.getShadowMap().bind();
        for (int i = 0; i < entities.size(); i++) {
            if (i == targetEntity.getId()) continue;

            Transform transform = entities.get(i).getComponent(Transform.class);
            Drawable drawable = entities.get(i).getComponent(Drawable.class);
            if (transform == null || drawable == null) continue;

            TransformC finalTransform = new Transform(transform).compose(drawable.getModel().getInitialTransform());
            renderer.render(finalTransform, drawable.getModel(), shader, null);
        }
        component.getShadowMap().unbind();
    }

}
