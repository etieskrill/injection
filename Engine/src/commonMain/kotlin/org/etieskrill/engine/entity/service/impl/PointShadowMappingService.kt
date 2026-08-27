package org.etieskrill.engine.entity.service.impl

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.PointLightComponent
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.engine.graphics.animation.Animator
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PipelineConfig
import org.etieskrill.engine.graphics.renderer.Renderer
import org.etieskrill.engine.graphics.shader.impl.DepthCubeMapArrayShader
import org.joml.Matrix4f
import org.joml.Matrix4fc

class PointShadowMappingService(
    private val renderer: Renderer,
    private val shader: DepthCubeMapArrayShader
) : Service {

    private val DUMMY_MATRIX: Matrix4fc = Matrix4f()

    private val updateFrequency = 2
    private var cycle = 0

    override fun canProcess(entity: Entity) =
        entity.hasComponents<PointLightComponent>()
                && entity.getComponent<PointLightComponent>()!!.shadowMap != null

    override fun preProcess(delta: Double, entities: List<Entity>) {
        if (++cycle >= updateFrequency) {
            cycle = 0
        } else return

        entities.mapNotNull { it.getComponent<PointLightComponent>() }
            .mapNotNull { it.shadowMap }
            .distinct()
            .forEach { it.clear() }
    }

    override fun process(targetEntity: Entity, entities: List<Entity>, delta: Double) {
        if (cycle++ != 0) return

        val component = targetEntity.getComponent<PointLightComponent>()!!

        shader.apply {
            TODO("fix")
//            light = component.light
//            index = component.shadowMapIndex!!
//            shadowCombined = component.shadowCombinedMatrices!! as Array<Matrix4fc>
//            farPlane = component.shadowFarPlane!!
        }

        entities.filterNot { it.id == targetEntity.id }
            .forEach { entity ->
                val transform = entity.getComponent<Transform>() ?: return@forEach
                val drawable = entity.getComponent<Drawable>() ?: return@forEach

                entity.getComponent<Animator>()?.let { //TODO animated shader
//                    shader.boneMatrices = it.transformMatricesArray
                }
//                shader.combined = DUMMY_MATRIX

                drawable.model.nodes.forEach { node ->
//                    shader.model = (transform * node.getGlobalTransform()).matrix

                    node.meshes.forEach { mesh ->
                        val pipeline = Pipeline(
                            mesh.vao,
                            PipelineConfig(),
                            shader,
                            component.shadowMap!!
                        )

                        renderer.render(pipeline)
                    }
                }
            }
    }

}
