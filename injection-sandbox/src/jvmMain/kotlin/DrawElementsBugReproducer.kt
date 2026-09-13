import injection.sandbox.math.rad
import org.etieskrill.engine.application.App
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.model.box
import org.etieskrill.engine.graphics.model.model
import org.etieskrill.engine.graphics.pipeline.Pipeline
import org.etieskrill.engine.graphics.pipeline.PipelineConfig
import org.etieskrill.engine.graphics.shader.impl.SingleColourShader
import org.joml.Math.toRadians
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector4f

fun main() = DrawElementsBugReproducer.run()

object DrawElementsBugReproducer : App() {
    val camera = PerspectiveCamera(window.size).apply {
        orbit = true
        orbitDistance = 5f
    }

    val model = model("model") {
        box(Vector3f(-1f), Vector3f(1f))
    }

    val pipeline = Pipeline(
        model.rootNode.children.flatMap { it.meshes }[0].vao,
        PipelineConfig(),
        SingleColourShader(),
        window.screenBuffer
    )

    override fun loop(delta: Double) {
        camera.rotation = Quaternionf()
            .rotateY(pacer.timeElapsedTotalSeconds.rad)
            .rotateX(toRadians(30f))

        pipeline.shader.setUniform("model", model.rootNode.children[0].getGlobalTransform().matrix)
        pipeline.shader.setUniform("combined", camera.combined)
        pipeline.shader.setUniform("colour", Vector4f(1f, 0f, 0f, 1f))

        renderer.render(pipeline)
    }
}
