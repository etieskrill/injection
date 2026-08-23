package org.etieskrill.engine.entity.service.impl

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.engine.graphics.camera.Camera
import org.etieskrill.engine.graphics.particle.ParticleNode
import org.etieskrill.engine.graphics.particle.ParticleRenderer
import kotlin.reflect.KClass

class ParticleUpdateService : Service {

    override fun canProcess(entity: Entity) = entity.hasComponents<ParticleNode>()

    override fun process(
        targetEntity: Entity,
        entities: List<Entity>,
        delta: Double
    ) {
        val node = targetEntity.getComponent<ParticleNode>()!!

        node.update(delta)
    }
}

class ParticleRenderService(
    private val renderer: ParticleRenderer,
    private val camera: Camera
) : Service {

    override fun canProcess(entity: Entity) = entity.hasComponents<Transform, ParticleNode>()

    override fun process(targetEntity: Entity, entities: List<Entity>, delta: Double) {
        //FIXME use transform in as additional base transform
        val node = targetEntity.getComponent<ParticleNode>()!!

        renderer.renderParticles(node, camera)
    }

    override val runAfter: Set<KClass<out Service>>
        get() = setOf(RenderService::class)

}
