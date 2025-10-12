package org.etieskrill.engine.scene.component

import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.text.Fonts
import org.etieskrill.engine.graphics.text.TextEditor
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.Keys
import org.joml.Vector2ic
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.minus
import org.joml.plus

class TextField : Node<TextField>() {

    private val font = Fonts.getDefault()

    var textEditor = TextEditor(font)

    override fun render(batch: Batch) {
        if (textEditor.selector.hasSelection)
            drawSelection(batch, textEditor.toString(), textEditor.selector.start, textEditor.selector.end)

        batch.renderText(textEditor.toString(), font, absolutePosition)
        val absoluteCursorPosition = batch.getAbsoluteCursorPosition(
            textEditor.cursor.position,
            textEditor.toString(),
            font,
            size
        )
        if (absoluteCursorPosition == null) { //TODO remove
            batch.getAbsoluteCursorPosition(textEditor.cursor.position, textEditor.toString(), font, size)
            println(textEditor.cursor.position)
            println("'${textEditor}'")
        }
        batch.renderBox(
            Vector3f(absolutePosition + absoluteCursorPosition!!.add(0f, 0.2f * font.lineHeight), 0f),
            Vector3f(font.lineHeight / 12f, font.lineHeight.toFloat(), 0f),
            Vector4f(1f)
        )
    }

    private fun drawSelection(batch: Batch, text: String, start: Vector2ic, end: Vector2ic) {
        if (start.y() != end.y()) TODO("Multiline selection")

        val absStartPos = batch.getAbsoluteCursorPosition(start, text, font, size)!!
        val absEndPos = batch.getAbsoluteCursorPosition(end, text, font, size)!!
        val absSize = absEndPos - absStartPos
        batch.renderBox(
            Vector3f(absolutePosition + absStartPos.add(0f, 0.2f * font.lineHeight), 0f),
            Vector3f(absSize.x, font.lineHeight.toFloat() + absSize.y, 0f),
            Vector4f(0.4f, 0.4f, 1f, 1f)
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

        val ctrl = key.modifiers and Keys.Mod.CONTROL.glfwKey != 0
        val shift = key.modifiers and Keys.Mod.SHIFT.glfwKey != 0

        when (key.value) { //FIXME i hate my trash fucking api
            Keys.BACKSPACE.input.value -> textEditor.remove(ctrl = ctrl)
            Keys.ENTER.input.value -> textEditor += '\n'
            Keys.UP.input.value -> textEditor.cursor.up()
            Keys.DOWN.input.value -> textEditor.cursor.down()
            Keys.LEFT.input.value -> textEditor.cursor.left(ctrl = ctrl, select = shift)
            Keys.RIGHT.input.value -> textEditor.cursor.right(ctrl = ctrl, select = shift)
            Keys.HOME.input.value -> textEditor.cursor.home(ctrl = ctrl)
            Keys.END.input.value -> textEditor.cursor.end(ctrl = ctrl)
            else -> return false
        }
        return true
    }

    override fun handleCharacter(char: Char): Boolean {
        textEditor += char
        return true
    }

}