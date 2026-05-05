package org.etieskrill.engine.scene.container

import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.text.Fonts
import org.etieskrill.engine.graphics.texture.Textures
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.scene.Node
import org.etieskrill.engine.scene.Node.ScaleMode.*
import org.etieskrill.engine.scene.getPreferredNodePosition
import org.joml.Math.toRadians
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.minusAssign

/**
 * A collapsible node with a draggable widget bar containing a single child.
 */
open class WidgetContainer(
    child: Node<*>? = null
) : Node<WidgetContainer>() {

    companion object {
        private val WIDGET_BAR_COLOUR = Vector4f(128 / 255f, 0f, 0f, 1f) //#800000
        private const val WIDGET_BAR_HEIGHT = 20
        private val WIDGET_CHEVRON_COLOUR = Vector4f(1f)
        private const val WIDGET_CHEVRON_MARGIN = 2
    }

    var child: Node<*>? = child
        set(value) {
            invalidate()
            value?.parent = this
            field = value
        }

    init {
        this.child = child //to call property getter
    }

    var collapsed = false
        set(value) {
            invalidate()
            field = value
        }

    var text: String? = null

    private val actualSize = Vector2f(-1f)
    private val barHeight = WIDGET_BAR_HEIGHT
    private val chevronIcon = Textures.ofFile("textures/icons/chevron-down-solid-black.png")
    private val titleFont = Fonts.getDefault((barHeight) - 4)

    override fun update(delta: Double) {
        child?.update(delta)
    }

    override fun computeFixedSizes() { //FIXME bar height is still ignored somehow
        if (!shouldFormat) return

        child?.computeFixedSizes()

        computedFixedSize = false

        if (collapsed) {
            when (scaleMode) {
                FIXED -> {
                    formattedSize.set(size.x, barHeight.toFloat())
                    computedFixedSize = true
                }

                CONTENT -> {
                    when {
                        child == null -> {
                            formattedSize.set(100f, barHeight.toFloat())
                            computedFixedSize = true
                        }

                        child!!.scaleMode == GROW -> TODO("ScaleMode.GROW for WidgetContainer")

                        child!!.scaleMode != GROW -> {
                            if (!child!!.computedFixedSize) throw IllegalStateException("Child size was not computed")

                            formattedSize.set(child!!.formattedSize.x, barHeight.toFloat())
                            computedFixedSize = true
                        }
                    }
                }

                GROW -> TODO("ScaleMode.GROW for WidgetContainer")
            }
            return
        }

        when (scaleMode) {
            FIXED -> {
                formattedSize.set(size.x, size.y + barHeight)
                computedFixedSize = true
            }

            CONTENT -> {
                when {
                    child == null -> {
                        formattedSize.set(100f, barHeight.toFloat() + 5f)
                        computedFixedSize = true
                    }

                    child!!.scaleMode == GROW -> TODO("ScaleMode.GROW for WidgetContainer")

                    child!!.scaleMode != GROW -> {
                        if (!child!!.computedFixedSize) throw IllegalStateException("Child size was not computed")

                        formattedSize.set(child!!.formattedSize.x, child!!.formattedSize.y + barHeight)
                        computedFixedSize = true
                    }
                }
            }

            GROW -> TODO("ScaleMode.GROW for WidgetContainer")
        }
    }

    override fun layout() {
        if (!shouldFormat()) return

        child?.let {
            it.layout()
            it.position = getPreferredNodePosition(formattedSize, it).apply { y += barHeight.toFloat() }
        }
    }

    override fun render(batch: Batch) {
        val position = absolutePosition

        batch.renderBox(
            Vector3f(position, 0f),
            Vector3f(formattedSize.x, barHeight.toFloat(), 0f),
            WIDGET_BAR_COLOUR
        )
        batch.blit(
            chevronIcon,
            Vector2f(position).add(WIDGET_CHEVRON_MARGIN.toFloat(), WIDGET_CHEVRON_MARGIN.toFloat()),
            Vector2f(barHeight - 2f * WIDGET_CHEVRON_MARGIN),
            if (collapsed) toRadians(90f) else toRadians(-180f),
            WIDGET_CHEVRON_COLOUR
        )
        text?.takeIf { it.isNotBlank() }?.let {
            //TODO use label with autoscaling font instead of hardcoding - scale bar height too actually
            batch.renderText(text, titleFont, Vector2f(position).add(barHeight + 2f, -3f))
        }

        if (!collapsed) {
            if (renderedColour.w != 0f) {
                batch.renderBox(
                    Vector3f(absolutePosition, 0f).apply { y += barHeight.toFloat() },
                    Vector3f(formattedSize, 0f).apply { y -= barHeight.toFloat() },
                    renderedColour
                )
            }
            child?.render(batch)
        }
    }

    override fun handleHit(button: Key, action: Keys.Action, posX: Double, posY: Double): Boolean {
        if (!doesHit(posX, posY)) return false

        val position = absolutePosition
        if (action == Keys.Action.PRESS
            && posX > position.x && posX <= position.x + barHeight
            && posY > position.y && posY <= position.y + barHeight
        ) {
            collapsed = !collapsed
            requestFocus() //any reason for a resetFocus instead?
            return true
        }

        return child?.handleHit(button, action, posX, posY) ?: false //container itself is not hittable
    }

    override fun handleKey(key: Key, action: Keys.Action): Boolean =
        child?.handleKey(key, action) ?: false

    override fun handleDrag(deltaX: Double, deltaY: Double, posX: Double, posY: Double): Boolean {
        if (alignment != Alignment.FIXED_POSITION) return false

        if (!doesHit(posX, posY)) return false

        if (posX !in (position.x + barHeight)..(position.x + formattedSize.x)
            || posY !in position.y..(position.y + barHeight)
        ) {
            return false
        }

        position -= Vector2f(deltaX.toFloat(), deltaY.toFloat())

        return true
    }

}
