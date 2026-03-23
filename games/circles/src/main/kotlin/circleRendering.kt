package io.github.etieskrill.games.circles

import org.etieskrill.engine.graphics.Renderer
import org.joml.Vector2fc

fun Main.renderCircle(circle: Circle, position: Vector2fc, size: Float, renderer: Renderer) {
    drawCircle(position, size, 1f, renderer)
}

private fun Main.drawCircle(position: Vector2fc, size: Float, thickness: Float, renderer: Renderer) {
    pipeline.shader.apply {
        combined = camera.combined
        aspect = camera.aspectRatio
    }

    renderer.render(pipeline)
}
