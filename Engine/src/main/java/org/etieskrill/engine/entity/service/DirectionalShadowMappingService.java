package org.etieskrill.engine.entity.service;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.DirectionalLightComponent;
import org.etieskrill.engine.entity.component.Drawable;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.component.TransformC;
import org.etieskrill.engine.graphics.Renderer;
import org.etieskrill.engine.graphics.gl.shader.Shaders;

import java.util.List;

public class DirectionalShadowMappingService implements Service {

    private final Renderer renderer;
    private final Shaders.DepthShader shader;

    public DirectionalShadowMappingService(Renderer renderer, Shaders.DepthShader shader) {
        this.renderer = renderer;
        this.shader = shader;
    }

    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(DirectionalLightComponent.class);
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        //TODO "rendered camera" component to derive view frustum
//        DirectionalLight light = (DirectionalLight) target.get(DirectionalLight.class).getComponent();
        DirectionalLightComponent shadowMapComponent = targetEntity.getComponent(DirectionalLightComponent.class);

        shadowMapComponent.getShadowMap().clear();
        shadowMapComponent.getShadowMap().bind();
        for (int i = 0; i < entities.size(); i++) {
            //TODO abstracted access object for all components, buffering of components (and combinations thereof), can also help with the bloody casts
            if (i == targetEntity.getId()) continue;
            Transform transform = entities.get(i).getComponent(Transform.class);
            Drawable drawable = entities.get(i).getComponent(Drawable.class);
            if (transform == null || drawable == null) continue;
            TransformC finalTransform = new Transform(transform).compose(drawable.getModel().getInitialTransform());
            renderer.render(
                    finalTransform,
                    drawable.getModel(),
                    shader,
                    shadowMapComponent.getCombined()
            );
        }
        shadowMapComponent.getShadowMap().unbind();
    }

}
