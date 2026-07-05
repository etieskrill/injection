package org.etieskrill.engine.scene.element

import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.text.Font
import org.etieskrill.engine.graphics.text.Fonts
import org.etieskrill.engine.scene.Node
import org.etieskrill.engine.scene.Node.ScaleMode.*
import kotlin.math.max

open class Label(
    text: String? = null,
    val font: Font = Fonts.getDefault()
) : Node<Label>() {

    var text: String? = text
        set(value) {
            if (field != value) invalidate()
            field = value
        }

    init {
        this.text = text
    }

    override fun computeFixedSizes() {
        if (!shouldFormat()) return

        when (scaleMode) {
            FIXED -> {
                formattedSize = size
                computedFixedSize = true
            }

            CONTENT -> {
                if (text == null) {
                    formattedSize.set(0f)
                    computedFixedSize = true
                }

                var maxWidth = 0
                var width = 0
                var height = font.minLineHeight

                font.getGlyphs(text).forEach { glyph ->
                    width += glyph.advance.x().toInt()
                    when (glyph.character) {
                        '\n' -> {
                            height += font.getLineHeight()
                            maxWidth = max(maxWidth, width)
                            width = 0
                        }
                    }
                }
                maxWidth = max(maxWidth, width)

                formattedSize.set(
                    maxWidth.toFloat(),
                    height.toFloat()
                )  //TODO figure out or compute actual font line height and add toggle here
                computedFixedSize = true
            }

            GROW -> TODO("ScaleMode.GROW for Label")
        }
    }

    override fun render(batch: Batch) {
        text?.let { batch.renderText(it, font, absolutePosition) }
    }

}
