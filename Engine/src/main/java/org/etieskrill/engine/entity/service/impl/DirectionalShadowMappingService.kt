package org.etieskrill.engine.entity.service.impl

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.DirectionalLightComponent
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.engine.graphics.Renderer
import org.etieskrill.engine.graphics.animation.Animator
import org.etieskrill.engine.graphics.gl.shader.Shaders
import org.etieskrill.engine.graphics.gl.shader.impl.DepthAnimatedShader
import org.etieskrill.engine.graphics.gl.shader.impl.boneMatrices

const val DEFAULT_UPDATE_FREQUENCY = 2
//const val DEFAULT_ENTITY_UPDATE_FREQUENCY = 4

class DirectionalShadowMappingService(
    val renderer: Renderer,
    val depthShader: Shaders.DepthShader = Shaders.DepthShader(),
    val animatedDepthShader: DepthAnimatedShader = DepthAnimatedShader()
) : Service {

    //TODO make superclass for sparsely executed services and entity round robin services
    private val updateFrequency = DEFAULT_UPDATE_FREQUENCY;
    private var cycle = 0;

//    private final int entityUpdateFrequency = DEFAULT_UPDATE_FREQUENCY;
//    private final Map<Entity, Map<Entity, Integer>> updateCycleMaps = new HashMap<>();
//    private int newEntitySpread = 0;
//    private int entityCycle = 0;

    override fun canProcess(entity: Entity) = entity.hasComponents<DirectionalLightComponent>()

    override fun process(targetEntity: Entity, entities: List<Entity>, delta: Double) {
        //TODO "rendered camera" component to derive view frustum
//        DirectionalLight light = (DirectionalLight) target.get(DirectionalLight.class).getComponent();
        val shadowMapComponent = targetEntity.getComponent<DirectionalLightComponent>()!!

        if (++cycle >= updateFrequency) {
            cycle = 0;
        } else {
            return;
        }

        val shadowMap = shadowMapComponent.shadowMap ?: return
        shadowMap.clear()
        shadowMap.bind()

//        var updateCycles = updateCycleMaps.computeIfAbsent(targetEntity, entity -> new HashMap<>());
        for (entity in entities) {
            //TODO this would need at least a secondary buffer for swapping, and maybe even more for blending over time
//            if (!isEntityUpdateCycle(updateCycles, entity)) continue;

            //TODO abstracted access object for all components, buffering of components (and combinations thereof), can also help with the bloody casts
            if (entity.id == targetEntity.id) continue
            val transform = entity.getComponent<Transform>() ?: continue
            val drawable = entity.getComponent<Drawable>() ?: continue

            val shader = when (val animator = entity.getComponent<Animator>()) {
                null -> depthShader
                else -> animatedDepthShader.apply { boneMatrices = animator.transformMatricesArray }
            }

            renderer.render(
                transform,
                drawable.model,
                shader,
                shadowMapComponent.camera
            )
        }

        shadowMap.unbind()
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
