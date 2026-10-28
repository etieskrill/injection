package org.etieskrill.engine.scene.element

import net.bifreus.kml.Quatc
import net.bifreus.kml.Vec3
import net.bifreus.kml.Vec3c
import net.bifreus.kml.Vec4
import org.etieskrill.engine.graphics.model.loader.loadModel
import org.etieskrill.engine.graphics.pipeline.CullingMode
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PipelineConfig
import org.etieskrill.engine.graphics.shader.Shader
import org.etieskrill.engine.scene.Batch
import org.etieskrill.engine.scene.Node

class TranslateGizmo(
    translationCallback: (Vec3c) -> Unit,
) : Node<TranslateGizmo>() {

    private val arrowModel = loadModel("arrow.glb")
    private val arrowTransform = arrowModel.nodes[0].getGlobalTransform()

    private lateinit var pipeline: Pipeline<TranslateGizmoShader>

//    override fun handleDrag(deltaX: Double, deltaY: Double, posX: Double, posY: Double): Boolean {
//        return false
//    }

    override fun render(batch: Batch) {
        if (!::pipeline.isInitialized) {
            pipeline = Pipeline(
                arrowModel.nodes.first().meshes.first().vao,
                PipelineConfig(
                    cullingMode = CullingMode.NONE,
                    depthTest = false,
                    writeDepth = false,
                ),
                TranslateGizmoShader(),
                batch.frameBuffer
            )
        }

        pipeline.shader.setUniform("position", Vec3(0f, 0f, 0f))
        pipeline.shader.setUniform("model", arrowTransform.matrix)
        pipeline.shader.setUniform("combined", batch.combined)
        pipeline.shader.setUniform("colour", Vec4(1f, 0f, 0f, 0.6f))

        batch.render(pipeline)
    }

}

class TranslateGizmoShader : Shader(listOf("TranslateGizmo.glsl"), false)

class RotateGizmo(
    rotationCallback: (Quatc) -> Unit
) : Node<RotateGizmo>()
