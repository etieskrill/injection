package org.etieskrill.engine.entity.service.impl

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.component.WorldSpaceAABB
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.engine.graphics.camera.Camera
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.model.box
import org.etieskrill.engine.graphics.pipeline.CullingMode
import org.etieskrill.engine.graphics.pipeline.FillMode
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PipelineConfig
import org.etieskrill.engine.graphics.renderer.Renderer
import org.etieskrill.engine.graphics.shader.impl.WireframeShader
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.reflect.KClass

class BoundingBoxRenderService(
    val frameBuffer: FrameBuffer,
    val renderer: Renderer,
    val camera: Camera
) : Service {

    var renderBoundingBoxes = true //TODO enable/disable services as a whole

    private val shader = WireframeShader()

    private val box = box(Vector3f(1f))
    private val boundingBoxTransform = Transform()

    private val pipeline = Pipeline(
        box.nodes.flatMap { it.meshes }[0].vao,
        PipelineConfig(
            cullingMode = CullingMode.NONE,
            fillMode = FillMode.LINE
        ),
        shader,
        frameBuffer
    )

    override fun canProcess(entity: Entity) = entity.hasComponents<WorldSpaceAABB>()

    override fun process(targetEntity: Entity, entities: List<Entity>, delta: Double) {
        if (!renderBoundingBoxes) return

        val aabb = targetEntity.getComponent<WorldSpaceAABB>()!!
        aabb.center(boundingBoxTransform.position)
        aabb.getSize(boundingBoxTransform.scale)

        pipeline.shader.apply {
            setUniform("model", boundingBoxTransform)
            setUniform("combined", camera.combined)
            setUniform("colour", Vector4f(0f, 0f, 0f, 1f))
        }

        renderer.render(pipeline)
    }

    fun toggleRenderBoundingBoxes() {
        renderBoundingBoxes = !renderBoundingBoxes;
    }

    override val runAfter: Set<KClass<out Service>>
        get() = setOf(BoundingBoxService::class)

}
