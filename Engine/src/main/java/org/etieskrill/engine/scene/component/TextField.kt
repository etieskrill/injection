package org.etieskrill.engine.scene.component

import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.text.Fonts
import org.etieskrill.engine.graphics.text.TextEditor
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.Keys
import org.joml.Vector3f
import org.joml.plus

class TextField : Node<TextField>() {

    private val font = Fonts.getDefault()

    var textEditor = TextEditor(font)

    override fun render(batch: Batch) {
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

        val ctrl = key.modifiers and Keys.Mod.CONTROL.glfwKey != 0
        val shift = key.modifiers and Keys.Mod.SHIFT.glfwKey != 0

        when (key.value) { //FIXME i hate my trash fucking api
            Keys.BACKSPACE.input.value -> textEditor.remove(ctrl = ctrl)
            Keys.ENTER.input.value -> textEditor += '\n'
            Keys.UP.input.value -> textEditor.cursor.up()
            Keys.DOWN.input.value -> textEditor.cursor.down()
            Keys.LEFT.input.value -> textEditor.cursor.left(ctrl = ctrl)
            Keys.RIGHT.input.value -> textEditor.cursor.right(ctrl = ctrl)
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