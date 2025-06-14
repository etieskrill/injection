package org.etieskrill.engine.scene.component

import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.etieskrill.engine.graphics.model.Model
import org.etieskrill.engine.graphics.pipeline.CullingMode
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PipelineConfig
import org.joml.Quaternionfc
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4f

class TranslateGizmo(
    translationCallback: (Vector3fc) -> Unit
) : Node<TranslateGizmo>() {

    private val arrowModel = Model.ofFile("arrow.glb")
    private val arrowTransform = arrowModel.nodes.first().hierarchyTransform

    private val pipeline = Pipeline<TranslateGizmoShader>(
        arrowModel.nodes.first().meshes.first().vao,
        PipelineConfig(
            cullingMode = CullingMode.NONE,
            depthTest = false,
            writeDepth = false,
        ),
        TranslateGizmoShader(),
        null
    )

//    override fun handleDrag(deltaX: Double, deltaY: Double, posX: Double, posY: Double): Boolean {
//        return false
//    }

    override fun render(batch: Batch) {
        pipeline.shader.setUniform("position", Vector3f(0f, 0f, 0f))
        pipeline.shader.setUniform("model", arrowTransform.matrix)
        pipeline.shader.setUniform("combined", batch.combined)
        pipeline.shader.setUniform("colour", Vector4f(1f, 0f, 0f, 0.6f))

        batch.render(pipeline)
    }

}

class TranslateGizmoShader() : ShaderProgram(listOf("TranslateGizmo.glsl"), false)

class RotateGizmo(
    rotationCallback: (Quaternionfc) -> Unit
) : Node<RotateGizmo>()
