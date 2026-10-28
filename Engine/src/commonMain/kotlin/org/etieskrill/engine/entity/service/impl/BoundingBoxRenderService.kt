package org.etieskrill.engine.entity.service.impl

import net.bifreus.kml.Vec3
import net.bifreus.kml.Vec4
import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.component.WorldSpaceAABB
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.engine.graphics.camera.Camera
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.model.box
import org.etieskrill.engine.graphics.model.model
import org.etieskrill.engine.graphics.pipeline.CullingMode
import org.etieskrill.engine.graphics.pipeline.FillMode
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PipelineConfig
import org.etieskrill.engine.graphics.renderer.Renderer
import org.etieskrill.engine.graphics.shader.impl.WireframeShader
import kotlin.reflect.KClass

class BoundingBoxRenderService(
    val frameBuffer: FrameBuffer,
    val renderer: Renderer,
    val camera: Camera
) : Service {

    var renderBoundingBoxes = true //TODO enable/disable services as a whole

    private val shader = WireframeShader()

    private val box = model("box") { box(Vec3(-0.5f), Vec3(0.5f)) }
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
        boundingBoxTransform.position = aabb.center
        boundingBoxTransform.scale = aabb.size

        pipeline.shader.apply {
            setUniform("model", boundingBoxTransform)
            setUniform("combined", camera.combined)
            setUniform("colour", Vec4(0f, 0f, 0f, 1f))
        }

        renderer.render(pipeline)
    }

    fun toggleRenderBoundingBoxes() {
        renderBoundingBoxes = !renderBoundingBoxes;
    }

    override val runAfter: Set<KClass<out Service>>
        get() = setOf(BoundingBoxService::class)

}
