package org.etieskrill.engine.entity.service.impl;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.DirectionalLightComponent;
import org.etieskrill.engine.entity.component.Drawable;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.service.Service;
import org.etieskrill.engine.graphics.Renderer;
import org.etieskrill.engine.graphics.animation.Animator;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.Shaders;
import org.etieskrill.engine.graphics.gl.shader.impl.DepthAnimatedShader;
import org.etieskrill.engine.graphics.gl.shader.impl.DepthAnimatedShaderKt;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.List;

public class DirectionalShadowMappingService implements Service {

    public static final int DEFAULT_UPDATE_FREQUENCY = 2;
//    public static final int DEFAULT_ENTITY_UPDATE_FREQUENCY = 4;

    private final Renderer renderer;
    private final @Nullable Shaders.DepthShader shader;
    private final Shaders.DepthShader depthShader;
    private final DepthAnimatedShader depthAnimatedShader;

    //TODO make superclass for sparsely executed services and entity round robin services
    private final int updateFrequency = DEFAULT_UPDATE_FREQUENCY;
    private int cycle = 0;

//    private final int entityUpdateFrequency = DEFAULT_UPDATE_FREQUENCY;
//    private final Map<Entity, Map<Entity, Integer>> updateCycleMaps = new HashMap<>();
//    private int newEntitySpread = 0;
//    private int entityCycle = 0;

    public DirectionalShadowMappingService(Renderer renderer) {
        this.renderer = renderer;
        this.shader = null;
        this.depthShader = new Shaders.DepthShader();
        this.depthAnimatedShader = new DepthAnimatedShader();
    }

    public DirectionalShadowMappingService(Renderer renderer, Shaders.@Nullable DepthShader shader) {
        this.renderer = renderer;
        this.shader = shader;
        this.depthShader = null;
        this.depthAnimatedShader = null;
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

        if (++cycle >= updateFrequency) {
            cycle = 0;
        } else {
            return;
        }
        shadowMapComponent.getShadowMap().clear();
        shadowMapComponent.getShadowMap().bind();

//        var updateCycles = updateCycleMaps.computeIfAbsent(targetEntity, entity -> new HashMap<>());
        for (Entity entity : entities) {
            //TODO this would need at least a secondary buffer for swapping, and maybe even more for blending over time
//            if (!isEntityUpdateCycle(updateCycles, entity)) continue;

            //TODO abstracted access object for all components, buffering of components (and combinations thereof), can also help with the bloody casts
            if (entity.getId() == targetEntity.getId()) continue;
            Transform transform = entity.getComponent(Transform.class);
            Drawable drawable = entity.getComponent(Drawable.class);
            if (transform == null || drawable == null) continue;

            ShaderProgram shader = this.shader;

            if (shader == null) {
                shader = depthShader;

                Animator animator = entity.getComponent(Animator.class);
                if (animator != null) {
                    //TOTO figure some compile-time thing to ensure property exists on shader, probably kotlin interface property?
                    //actually you can just invert the control structure (first set uniforms then upcast), which should work for most cases
                    DepthAnimatedShaderKt.setBoneMatrices(depthAnimatedShader, (Matrix4f[]) animator.getTransformMatricesArray());
                    shader = depthAnimatedShader;
                }
            }

            renderer.render(
                    transform,
                    drawable.getModel(),
                    shader,
                    shadowMapComponent.getCamera()
            );
        }
        shadowMapComponent.getShadowMap().unbind();
    }

//    private boolean isEntityUpdateCycle(Map<Entity, Integer> updateCycles, Entity entity) {
//        Integer updateCycle = updateCycles.get(entity);
//        if (updateCycle == null) {
//            updateCycles.put(entity, newEntitySpread);
//            newEntitySpread = ++newEntitySpread % updateFrequency;
//        } else if (++updateCycle >= updateFrequency) {
//            updateCycles.put(entity, 0);
//        } else {
//            updateCycles.put(entity, updateCycle);
//            return false;
//        }
//        return true;
//    }

}
