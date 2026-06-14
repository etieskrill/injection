package io.github.etieskrill.games.circles

import org.etieskrill.engine.graphics.Renderer
import org.joml.Math.lerp
import org.joml.Math.toRadians
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector4f
import org.joml.Vector4fc
import org.joml.plus
import org.joml.times
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

fun Main.renderCircle(
    circle: Circle,
    position: Vector2fc,
    size: Float,
    renderer: Renderer,
    colourOverride: Vector4fc? = null
) {
    if (colourOverride != null) TODO()

    val sdfs = mutableListOf<SDFShader.SDFVertex>()
    renderCircleSlot(circle, position, size, sdfs)

    sdfBuffer.setData(sdfs)

    pipeline.shader.apply {
        sdfLayers = sdfBuffer
        combined = camera.combined
        aspect = camera.aspectRatio
    }

    renderer.render(pipeline)
}

private fun renderCircleSlot(
    rune: CircleSlot,
    position: Vector2fc,
    size: Float,
    sdfs: MutableList<SDFShader.SDFVertex>
) {
    when (rune) {
        is PrimaryRune, is ReceptacleRune, is StreamRune -> {
            val colour = rune.storedVis.maxByOrNull { it.value }?.key?.colour?.let { Vector4f(it) } ?: Vector4f(1f)
            colour.w *= lerp(0.2f, 1f, (rune.storedVis.maxByOrNull { it.value }?.value ?: 0f) / rune.visCapacity)

            //TODO draw rune
            // - add rune atlas
            drawCircle(position, 0.7f * size, 0.005f, colour, sdfs)
        }

        is Circle -> {
            val colour = rune.storedVis.maxByOrNull { it.value }?.key?.colour?.let { Vector4f(it) } ?: Vector4f(1f)
            colour.w *= lerp(0.2f, 1f, (rune.storedVis.maxByOrNull { it.value }?.value ?: 0f) / rune.visCapacity)

            for (i in rune.auxRunes.indices) {
                drawCircle(position, size + (0.2f * size * i), 0.005f, colour, sdfs)
            }

            ring@ for (ringIndex in 0..<rune.auxRunes.size) {
                val runeAngleStep = 360 / max(1, rune.runes.size)
                for ((runeIndex, runeAngle) in (0..<360 step runeAngleStep).withIndex()) {
                    val auxRunes = rune.auxRunes[ringIndex][runeIndex]
                    if (auxRunes.isEmpty()) continue@ring

                    val auxRuneAngleStep = runeAngleStep / max(1, auxRunes.size)

                    for ((auxRuneIndex, auxRuneAngle) in (0..<360 step auxRuneAngleStep).withIndex()) {
                        val angle = toRadians(runeAngle + auxRuneAngle.toFloat())
                        val auxPos =
                            Vector2f(cos(angle), sin(angle)) * (size - 0.075f) + position

                        //TODO draw rune with rotation
                        drawCircle(auxPos, 0.15f * size, 0.005f, colour, sdfs, true)
                    }

                    if (rune.runes.isNotEmpty()) {
                        val angle = toRadians(runeAngle.toFloat())
                        val primPos = Vector2f(cos(angle), sin(angle)) * size + position //TODO determine ring pos
                        drawCircle(primPos, 0.175f * size, 0.005f, colour, sdfs, true)
                        rune.runes[runeIndex]?.let { renderCircleSlot(it, primPos, 0.175f * size, sdfs) }
                    }
                }
            }

            rune.focalRune?.let { renderCircleSlot(it, position, size, sdfs) }
        }
    }
}

private fun drawCircle(
    position: Vector2fc,
    size: Float,
    thickness: Float,
    colour: Vector4fc,
    sdfs: MutableList<SDFShader.SDFVertex>,
    opaque: Boolean = false
) {
    if (opaque) {
        sdfs += SDFShader.SDFVertex(
            position = position, rotation = 0f, size = size, thickness = thickness, glowStrength = 0f,
            colour = Vector4f(1f),
            shapeType = SDFShader.SHAPE_CIRCLE, styleType = SDFShader.STYLE_FILLED, layerType = SDFShader.LAYER_SUB,
            blendStrength = 0f,
        )
    }
    sdfs += SDFShader.SDFVertex(
        position = position, rotation = 0f, size = size, thickness = thickness, glowStrength = 0.02f, colour = colour,
        shapeType = SDFShader.SHAPE_CIRCLE, styleType = SDFShader.STYLE_LINE, layerType = SDFShader.LAYER_ADD,
        blendStrength = 0f,
    )
}
