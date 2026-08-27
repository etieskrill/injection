package org.etieskrill.engine.entity.service.impl

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.DirectionalLightComponent
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.engine.graphics.animation.Animator
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PipelineConfig
import org.etieskrill.engine.graphics.renderer.Renderer
import org.etieskrill.engine.graphics.shader.impl.DepthAnimatedShader
import org.etieskrill.engine.graphics.shader.impl.DepthShader

const val DEFAULT_UPDATE_FREQUENCY = 2
//const val DEFAULT_ENTITY_UPDATE_FREQUENCY = 4

class DirectionalShadowMappingService(
    val renderer: Renderer,
    val depthShader: DepthShader = DepthShader(),
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

        val shadowMapComponent = targetEntity.getComponent<DirectionalLightComponent>()!!

        if (++cycle >= updateFrequency) {
            cycle = 0;
        } else {
            return
        }

        val shadowMap = shadowMapComponent.shadowMap ?: return
        shadowMap.clear()

//        var updateCycles = updateCycleMaps.computeIfAbsent(targetEntity, entity -> new HashMap<>());
        for (entity in entities) {
            //TODO this would need at least a secondary buffer for swapping, and maybe even more for blending over time
//            if (!isEntityUpdateCycle(updateCycles, entity)) continue;

            //TODO abstracted access object for all components, buffering of components (and combinations thereof), can also help with the bloody casts
            if (entity.id == targetEntity.id) continue
            val transform = entity.getComponent<Transform>() ?: continue
            val drawable = entity.getComponent<Drawable>() ?: continue
            val boneMatrices = entity.getComponent<Animator>()?.transformMatricesArray

            for (node in drawable.model.nodes) {
                val modelTransform = transform * node.getGlobalTransform()

                for (mesh in node.meshes) {

                    //FIXME i dunno about this
                    val pipeline = Pipeline(
                        mesh.vao,
                        PipelineConfig(),
                        when (boneMatrices) {
                            null -> depthShader.apply {
                                model = modelTransform.matrix
                                combined = shadowMapComponent.camera!!.combined
                            }

                            else -> animatedDepthShader.apply {
                                model = modelTransform.matrix
                                combined = shadowMapComponent.camera!!.combined
                                this.boneMatrices = boneMatrices
                            }
                        },
                        shadowMap
                    )

                    renderer.render(pipeline)
                }
            }
        }
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
