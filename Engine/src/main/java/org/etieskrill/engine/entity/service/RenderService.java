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
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.joml.Vector2ic;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.lwjgl.opengl.GL11C.glViewport;

public class RenderService implements Service {

    protected final GLRenderer renderer;
    private final Camera camera;
    private final Vector2ic windowSize;
    private final Shaders.StaticShader shader;
    private final Shaders.LightSourceShader lightSourceShader;

    private final ShaderParams shaderParams;

    private final AtomicReference<Transform> cachedTransform;

    //TODO remove jury-rigged service
    // - "inner services"?
    // - service groups?
    // - global render state -> context as entity?
    private final BoundingBoxRenderService boundingBoxRenderService;

    public RenderService(GLRenderer renderer, Camera camera, Vector2ic windowSize) {
        this.renderer = renderer;
        this.camera = camera;
        this.windowSize = windowSize;
        this.shader = new Shaders.StaticShader();
        this.lightSourceShader = new Shaders.LightSourceShader();

        this.shaderParams = new ShaderParams(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashSet<>());

        this.cachedTransform = new AtomicReference<>(new Transform());

        this.boundingBoxRenderService = new BoundingBoxRenderService(renderer, camera);
    }

    private record ShaderParams(
            Map<String, Object> uniformBindings,
            Map<String, Object[]> uniformArrayBindings,
            Map<String, AbstractTexture> textureBindings,
            Set<ShaderProgram> configuredShaders
    ) {
        void clear() {
            uniformBindings.clear();
            uniformArrayBindings.clear();
            textureBindings.clear();
            configuredShaders.clear();
        }

        void addUniform(String name, Object value) {
            uniformBindings.put(name, value);
        }

        void addUniformArray(String name, Object... values) {
            uniformArrayBindings.put(name, values);
        }

        void addTexture(String name, AbstractTexture texture) {
            textureBindings.put(name, texture);
        }

        boolean isConfigured(ShaderProgram shader) {
            return !configuredShaders.add(shader);
        }
    }

    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(Transform.class, Drawable.class);
    }

    @Override
    public void preProcess(List<Entity> entities) {
//        renderer.prepare(); //TODO make separate renderbuffer and then somehow combine with base buffer
        glViewport(0, 0, windowSize.x(), windowSize.y()); //TODO move to framebuffer

        shaderParams.clear();

        for (Entity entity : entities) {
            DirectionalLightComponent directionalLightComponent = entity.getComponent(DirectionalLightComponent.class);
            if (directionalLightComponent == null) continue;
            //TODO expand to multiple directional lights
            shaderParams.addUniform("hasShadowMap", directionalLightComponent.getShadowMap() != null);
            if (directionalLightComponent.getShadowMap() != null) {
                shaderParams.addTexture("shadowMap", directionalLightComponent.getShadowMap().getTexture());
                shaderParams.addUniform("lightCombined", directionalLightComponent.getCombined());
            }
            shaderParams.addUniformArray("globalLights", directionalLightComponent.getDirectionalLight());
            break;
        }

        PointLightComponent[] pointLightComponents = entities.stream()
                .map(entity -> entity.getComponent(PointLightComponent.class))
                .filter(Objects::nonNull)
                .toArray(PointLightComponent[]::new);

        if (pointLightComponents.length > 0) {
            shaderParams.addUniformArray("lights", (Object[]) Arrays.stream(pointLightComponents)
                    .map(PointLightComponent::getLight)
                    .toArray(PointLight[]::new));
        }

        AtomicBoolean hasPointShadowMap = new AtomicBoolean(false);
        AtomicInteger numPointShadowMaps = new AtomicInteger();
        Arrays.stream(pointLightComponents)
                .map(PointLightComponent::getShadowMap)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(pointShadowMapArray -> {
                    hasPointShadowMap.set(true);
                    shaderParams.addTexture(
                            "pointShadowMaps" + numPointShadowMaps.getAndIncrement(),
                            pointShadowMapArray.getTexture());
                });
        shaderParams.addUniform("hasPointShadowMaps", hasPointShadowMap.get());

        shaderParams.addUniform("viewPosition", camera.getPosition());

        for (Entity entity : entities) {
            if (boundingBoxRenderService.canProcess(entity)) {
                boundingBoxRenderService.process(entity, entities, 0);
            }
        }
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        Boolean enabled = targetEntity.getComponent(Boolean.class);
        if (enabled != null && !enabled) return;

        Drawable drawable = targetEntity.getComponent(Drawable.class);
        if (!drawable.isVisible()) return;

        Transform transform = targetEntity.getComponent(Transform.class);
        final Transform finalTransform = transform;
        transform = cachedTransform.updateAndGet(t ->
                t.set(finalTransform).compose(drawable.getModel().getInitialTransform())
        );

        ShaderProgram shader = getConfiguredShader(targetEntity, drawable);
        if (!drawable.isDrawWireframe()) {
            renderer.render(transform, drawable.getModel(), shader, camera);
        } else {
            renderer.renderWireframe(transform, drawable.getModel(), shader, camera);
        }
    }

    private ShaderProgram getConfiguredShader(Entity entity, Drawable drawable) {
        if (drawable.getShader() != null) {
            configureShader(drawable.getShader(), shaderParams);
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
            configureShader(shader, shaderParams);
            shader.setTextureScale(drawable.getTextureScale());
            return shader;
        }
    }

    private void configureShader(ShaderProgram shader, ShaderParams params) {
        if (params.isConfigured(shader)) return;

        params.uniformBindings.forEach(shader::setUniformNonStrict);
        params.uniformArrayBindings.forEach(shader::setUniformArrayNonStrict);
        params.textureBindings.forEach((name, texture) -> renderer.bindNextFreeTexture(shader, name, texture));
    }

    @Override
    public Set<Class<? extends Service>> runAfter() {
        return Set.of(DirectionalShadowMappingService.class, PointShadowMappingService.class);
    }

    @Deprecated
    public BoundingBoxRenderService getBoundingBoxRenderService() {
        return boundingBoxRenderService;
    }

}
