package org.etieskrill.engine.scene.element

import org.etieskrill.engine.input.KeyEvent
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.KeyEventAction
import org.etieskrill.engine.input.ModifierKey
import org.etieskrill.engine.scene.Batch
import org.etieskrill.engine.scene.Node
import org.etieskrill.engine.time.LoopPacer
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector2i
import org.joml.Vector2ic
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.minus
import org.joml.plus

class TextField(
    private val pacer: LoopPacer
) : Node<TextField>() {

    private val font = Fonts.getDefault()

    var textEditor = TextEditor(font)
    var changeCallback: () -> Unit = {}

    init {
        colour = Vector4f(0.2f, 0.2f, 0.2f, 0.5f)
    }

    private val highlightColour = Vector4f(0.4f, 0.4f, 1f, 1f)

    override fun computeFixedSizes() {
        formattedSize.set(size)
    }

    override fun render(batch: Batch) {
        batch.renderBackground(
            absolutePosition,
            formattedSize,
            colour,
            2f,
            if (focused) highlightColour else Vector4f(0f)
        )

        if (textEditor.selector.hasSelection)
            drawSelection(
                batch,
                textEditor.toString(),
                textEditor.gridSize,
                textEditor.selector.start,
                textEditor.selector.end
            )

        batch.renderText(textEditor.toString(), font, absolutePosition)
        val absoluteCursorPosition = batch.getAbsoluteCursorPosition(
            textEditor.cursor.position,
            textEditor.toString(),
            font,
            formattedSize
        )

        if (focused && pacer.timerTimeSeconds % 1 < 0.5) {
            batch.renderBox(
                (Vector2f(absolutePosition) + absoluteCursorPosition!!).apply { y += 0.2f * font.lineHeight },
                Vector2f(font.lineHeight / 12f, font.lineHeight.toFloat()),
                Vector4f(1f)
            )
        }
    }

    private fun drawSelection(batch: Batch, text: String, lineLengths: List<Int>, start: Vector2ic, end: Vector2ic) {
        if (start.y() == end.y()) {
            val absStartPos = batch.getAbsoluteCursorPosition(start, text, font, formattedSize)!!
            val absEndPos = batch.getAbsoluteCursorPosition(end, text, font, formattedSize)!!
            drawSelectionLine(batch, absStartPos, absEndPos - absStartPos)
            return
        }

        val absStartPos = batch.getAbsoluteCursorPosition(start, text, font, formattedSize)!!
        val absEndPos =
            batch.getAbsoluteCursorPosition(Vector2i(lineLengths[start.y()], start.y()), text, font, formattedSize)!!
        drawSelectionLine(batch, absStartPos, absEndPos - absStartPos)

        for (i in start.y() + 1..end.y() - 1) {
            absStartPos.set(batch.getAbsoluteCursorPosition(Vector2i(0, i), text, font, formattedSize)!!)
            absEndPos.set(batch.getAbsoluteCursorPosition(Vector2i(lineLengths[i], i), text, font, formattedSize)!!)
            drawSelectionLine(batch, absStartPos, absEndPos - absStartPos)
        }

        absStartPos.set(batch.getAbsoluteCursorPosition(Vector2i(0, end.y()), text, font, formattedSize)!!)
        absEndPos.set(batch.getAbsoluteCursorPosition(end, text, font, formattedSize)!!)
        drawSelectionLine(batch, absStartPos, absEndPos - absStartPos)
    }

    private fun drawSelectionLine(batch: Batch, absStartPos: Vector2fc, absSize: Vector2fc) = batch.renderBox(
        (Vector2f(absolutePosition) + absStartPos).apply { y += 0.2f * font.lineHeight },
        Vector2f(absSize).apply { y += font.lineHeight.toFloat() },
        highlightColour
    )

    override fun handleHit(event: KeyEvent, posX: Double, posY: Double): Boolean {
        if (!doesHit(posX, posY)
            || event.key != Key.LEFT_MOUSE
            || event.action != KeyEventAction.RELEASE
        ) return false

        requestFocus()
        return true
    }

    override fun handleKey(event: KeyEvent): Boolean {
        if (!focused) return false
        if (event.action != KeyEventAction.RELEASE) return false

        val ctrl = ModifierKey.CONTROL in event.modifiers
        val shift = ModifierKey.SHIFT in event.modifiers

        when (event.key) {
            Key.BACKSPACE -> {
                textEditor.remove(ctrl = ctrl)
                changeCallback()
            }

            Key.ENTER -> {
                if (shift) textEditor.cursor.end()
                textEditor += '\n'
                changeCallback()
            }

            Key.UP -> textEditor.cursor.up(select = shift)
            Key.DOWN -> textEditor.cursor.down(select = shift)
            Key.LEFT -> textEditor.cursor.left(ctrl = ctrl, select = shift)
            Key.RIGHT -> textEditor.cursor.right(ctrl = ctrl, select = shift)
            Key.HOME -> textEditor.cursor.home(ctrl = ctrl, select = shift)
            Key.END -> textEditor.cursor.end(ctrl = ctrl, select = shift)
            else -> return false
        }
        return true
    }

    override fun handleCharacter(char: Char): Boolean {
        textEditor += char
        changeCallback()
        return true
    }

}