package org.etieskrill.engine.scene.component

import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.text.Fonts
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.Keys
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.plus
import kotlin.math.max

class TextField : Node<TextField>() {

    var text: String = ""

    private val font = Fonts.getDefault()

    override fun render(batch: Batch) {
        val cursorPosition = Vector2f()
        batch.renderText(text, font, absolutePosition, size, cursorPosition)
        batch.renderBox(
            Vector3f(absolutePosition + cursorPosition.add(0f, 0.2f * font.lineHeight), 0f),
            Vector3f(font.lineHeight / 12f, font.lineHeight.toFloat(), 0f),
            colour
        )
    }

    override fun handleHit(button: Key, action: Keys.Action, posX: Double, posY: Double): Boolean {
        if (!doesHit(posX, posY) || button != Keys.LEFT_MOUSE.input || action != Keys.Action.RELEASE) return false

        requestFocus()
        return true
    }

    override fun handleKey(key: Key, action: Keys.Action): Boolean {
        if (!focused) return false

        if (action != Keys.Action.RELEASE) return false
        when (key) {
            Keys.BACKSPACE.input -> text = text.substring(0..<max(0, text.length - 1))
            Keys.ENTER.input -> text += '\n'
            else -> return false
        }
        return true
    }

    override fun handleCharacter(char: Char): Boolean {
        text += char
        return true
    }

}