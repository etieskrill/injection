package org.etieskrill.engine.entity.service;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.DirectionalLightComponent;
import org.etieskrill.engine.entity.component.Drawable;
import org.etieskrill.engine.entity.component.PointLightComponent;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.data.PointLight;
import org.etieskrill.engine.graphics.gl.GLRenderer;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.Shaders;
import org.joml.Vector2ic;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.opengl.GL11C.glViewport;

public class RenderService implements Service {

    private final GLRenderer renderer;
    private final Camera camera;
    private final Vector2ic windowSize;
    private final Shaders.StaticShader shader;
    private final Shaders.LightSourceShader lightSourceShader;

    //TODO make blocking
    private final Transform cachedTransform;

    public RenderService(GLRenderer renderer, Camera camera, Vector2ic windowSize) {
        this.renderer = renderer;
        this.camera = camera;
        this.windowSize = windowSize;
        this.shader = new Shaders.StaticShader();
        this.lightSourceShader = new Shaders.LightSourceShader();

        this.cachedTransform = new Transform();
    }

    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(Transform.class, Drawable.class);
    }

    @Override
    public void preProcess(List<Entity> entities) {
//        renderer.prepare(); //TODO make separate renderbuffer and then somehow combine with base buffer
        glViewport(0, 0, windowSize.x(), windowSize.y());

        for (Entity entity : entities) {
            DirectionalLightComponent directionalLightComponent = entity.getComponent(DirectionalLightComponent.class);
            if (directionalLightComponent == null) continue;
            //TODO expand to multiple directional lights
            if (directionalLightComponent.getShadowMap() != null) {
                renderer.bindNextFreeTexture(shader, "u_ShadowMap", directionalLightComponent.getShadowMap().getTexture());
                shader.setUniform("u_LightCombined", directionalLightComponent.getCombined(), false);
            }
            shader.setGlobalLights(directionalLightComponent.getDirectionalLight());
            break;
        }

        PointLightComponent[] pointLightComponents = entities.stream()
                .map(entity -> entity.getComponent(PointLightComponent.class))
                .filter(Objects::nonNull)
                .toArray(PointLightComponent[]::new);

        if (pointLightComponents.length > 0) {
            shader.setLights(Arrays.stream(pointLightComponents)
                    .map(PointLightComponent::getLight)
                    .toArray(PointLight[]::new));
        }

        AtomicInteger numPointShadowMaps = new AtomicInteger();
        Arrays.stream(pointLightComponents)
                .map(PointLightComponent::getShadowMap)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(pointShadowMapArray -> renderer.bindNextFreeTexture(shader,
                        "pointShadowMaps" + numPointShadowMaps.getAndIncrement(),
                        pointShadowMapArray.getTexture()));

        shader.setViewPosition(camera.getPosition());
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        Boolean enabled = targetEntity.getComponent(Boolean.class);
        if (enabled != null && !enabled) return;

        Drawable drawable = targetEntity.getComponent(Drawable.class);
        if (!drawable.isVisible()) return;

        Transform transform = targetEntity.getComponent(Transform.class);
        transform = cachedTransform.set(transform)
                .compose(drawable.getModel().getInitialTransform());

        ShaderProgram shader = getConfiguredShader(targetEntity, drawable);
        if (!drawable.isDrawWireframe()) {
            renderer.render(transform, drawable.getModel(), shader, camera.getCombined());
        } else {
            renderer.renderWireframe(transform, drawable.getModel(), shader, camera.getCombined());
        }
    }

    private ShaderProgram getConfiguredShader(Entity entity, Drawable drawable) {
        if (drawable.getShader() != null) {
            return drawable.getShader();
        }

        DirectionalLightComponent directionalLightComponent = entity.getComponent(DirectionalLightComponent.class);
        PointLightComponent pointLightComponent = entity.getComponent(PointLightComponent.class);
        if (directionalLightComponent != null) {
            lightSourceShader.setLight(directionalLightComponent.getDirectionalLight());
            return lightSourceShader;
        } else if (pointLightComponent != null) {
            lightSourceShader.setLight(pointLightComponent.getLight());
            if (pointLightComponent.getFarPlane() != null)
                shader.setPointShadowFarPlane(pointLightComponent.getFarPlane()); //TODO make per-light?
            return lightSourceShader;
        } else {
            shader.setTextureScale(drawable.getTextureScale());
            return shader;
        }
    }

    @Override
    public Set<Class<? extends Service>> runAfter() {
        return Set.of(DirectionalShadowMappingService.class, PointShadowMappingService.class);
    }

}
