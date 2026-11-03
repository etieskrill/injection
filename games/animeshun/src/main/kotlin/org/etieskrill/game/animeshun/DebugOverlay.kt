package org.etieskrill.game.animeshun

import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.graphics.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.renderer.Renderer
import org.etieskrill.engine.graphics.text.TextRenderer
import org.etieskrill.engine.scene.Batch
import org.etieskrill.engine.scene.Scene
import org.etieskrill.engine.scene.container.Container
import org.etieskrill.engine.scene.element.Label
import org.etieskrill.engine.time.LoopPacer

class DebugOverlay(
    frameBuffer: FrameBuffer,
    val renderer: Renderer,
    textRenderer: TextRenderer,
    val pacer: LoopPacer
) : Scene(
    Batch(frameBuffer, renderer, textRenderer),
    Container(),
    OrthographicCamera(frameBuffer.size)
) {

    private val renderStatistics = Label()

    var cpuTime: Double = 0.0

    override fun update(delta: Double) {
        renderStatistics.text = """
            Render calls: ${renderer.renderCalls}
            Triangles drawn: ${renderer.trianglesDrawn}
            Average FPS: ${pacer.averageFPS.f(3)} (${(pacer.deltaTimeSeconds / 1000f).f(4, 1)}ms)
            CPU time: ${cpuTime.f(4, 1)}ms (${(renderer.gpuDelay.inWholeMicroseconds / 1000f).f(4, 1)}ms gpu sync time)
            GPU time: ${(renderer.averagedGpuTime.inWholeMicroseconds / 1000f).f(4, 1)}ms
        """.format().trimIndent()

        super.update(delta)
    }

}

private fun Float.f(places: Int, decimals: Int = 0) = String.format("%$places.${decimals}f", this)
private fun Double.f(places: Int, decimals: Int = 0) = String.format("%$places.${decimals}f", this)
