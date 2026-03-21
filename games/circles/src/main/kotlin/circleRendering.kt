package io.github.etieskrill.games.circles

import org.etieskrill.engine.graphics.Renderer
import org.joml.Math
import org.joml.Math.cos
import org.joml.Math.sin
import org.joml.Math.toRadians
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.plus
import org.joml.times

fun Main.renderCircle(circle: Circle, position: Vector2fc, size: Float, renderer: Renderer, hovering: Int?) {
    drawCircle(position, size, 1f, renderer)
    drawCircle(position, size * 0.85f, 1f, renderer)
    for (angle in 0..<360 step 60) {
        val runePos = position + Vector2f(
            cos(toRadians(angle.toFloat())),
            sin(toRadians(angle.toFloat()))
        ) * size * 0.91f * camera.viewportSize.x().toFloat() / 2f
        val runeSize = if (hovering == angle) 0.25f * size else 0.2f * size

        hideCirclePipeline.shader.apply {
            this.position = runePos
            this.size = runeSize
            combined = camera.combined
            aspect = camera.aspectRatio
        }
        renderer.render(hideCirclePipeline)

        drawCircle(runePos, runeSize, 1f, renderer)
    }
}

private fun Main.drawCircle(position: Vector2fc, size: Float, thickness: Float, renderer: Renderer) {
    pipeline.shader.apply {
        cursorPosition = position
        this.size = size
        combined = camera.combined
        aspect = camera.aspectRatio
    }

    renderer.render(pipeline)
}
