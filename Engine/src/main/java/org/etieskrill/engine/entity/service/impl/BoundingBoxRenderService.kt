package org.etieskrill.engine.entity.service.impl

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.component.WorldSpaceAABB
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.engine.graphics.Renderer
import org.etieskrill.engine.graphics.camera.Camera
import org.etieskrill.engine.graphics.gl.shader.Shaders
import org.etieskrill.engine.graphics.model.ModelFactory
import org.joml.Vector3f
import kotlin.reflect.KClass

class BoundingBoxRenderService(
    val renderer: Renderer,
    val camera: Camera
) : Service {

    var renderBoundingBoxes = true //TODO enable/disable services as a whole

    private val shader = Shaders.WireframeShader()

    private val box = ModelFactory.box(Vector3f(1f))
    private val boundingBoxTransform = Transform()

    override fun canProcess(entity: Entity) = entity.hasComponents<WorldSpaceAABB>()

    override fun process(targetEntity: Entity, entities: List<Entity>, delta: Double) {
        if (!renderBoundingBoxes) return

        val aabb = targetEntity.getComponent<WorldSpaceAABB>()!!
        aabb.center(boundingBoxTransform.position)
        aabb.getSize(boundingBoxTransform.scale)
        renderer.renderWireframe(boundingBoxTransform, box, shader, camera)
    }

    fun toggleRenderBoundingBoxes() {
        renderBoundingBoxes = !renderBoundingBoxes;
    }

    override val runAfter: Set<KClass<out Service>>
        get() = setOf(BoundingBoxService::class)

}
