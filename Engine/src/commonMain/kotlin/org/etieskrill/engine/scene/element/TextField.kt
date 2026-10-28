package org.etieskrill.engine.scene.element

import net.bifreus.kml.Vec2
import net.bifreus.kml.Vec2c
import net.bifreus.kml.Vec2i
import net.bifreus.kml.Vec2ic
import net.bifreus.kml.Vec4
import org.etieskrill.engine.graphics.text.Font
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.KeyEvent
import org.etieskrill.engine.input.KeyEventAction
import org.etieskrill.engine.input.ModifierKey
import org.etieskrill.engine.scene.Batch
import org.etieskrill.engine.scene.Node
import org.etieskrill.engine.text.TextEditor
import org.etieskrill.engine.time.LoopPacer

class TextField(
    private val pacer: LoopPacer
) : Node<TextField>() {

    private val font = Font.getDefault()

    var textEditor = TextEditor(font)
    var changeCallback: () -> Unit = {}

    init {
        colour = Vec4(0.2f, 0.2f, 0.2f, 0.5f)
    }

    private val highlightColour = Vec4(0.4f, 0.4f, 1f, 1f)

    override fun computeFixedSizes() {
        formattedSize(size)
    }

    override fun render(batch: Batch) {
        batch.renderBackground(
            absolutePosition,
            formattedSize,
            colour,
            2f,
            if (focused) highlightColour else Vec4(0f)
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
                (absolutePosition + absoluteCursorPosition!!).apply { y += 0.2f * font.lineHeight },
                Vec2(font.lineHeight / 12f, font.lineHeight.toFloat()),
                Vec4(1f)
            )
        }
    }

    private fun drawSelection(batch: Batch, text: String, lineLengths: List<Int>, start: Vec2ic, end: Vec2ic) {
        if (start.y == end.y) {
            val absStartPos = batch.getAbsoluteCursorPosition(start, text, font, formattedSize)!!
            val absEndPos = batch.getAbsoluteCursorPosition(end, text, font, formattedSize)!!
            drawSelectionLine(batch, absStartPos, absEndPos - absStartPos)
            return
        }

        val absStartPos = batch.getAbsoluteCursorPosition(start, text, font, formattedSize)!!
        val absEndPos =
            batch.getAbsoluteCursorPosition(Vec2i(lineLengths[start.y], start.y), text, font, formattedSize)!!
        drawSelectionLine(batch, absStartPos, absEndPos - absStartPos)

        for (i in start.y + 1..end.y - 1) {
            absStartPos(batch.getAbsoluteCursorPosition(Vec2i(0, i), text, font, formattedSize)!!)
            absEndPos(batch.getAbsoluteCursorPosition(Vec2i(lineLengths[i], i), text, font, formattedSize)!!)
            drawSelectionLine(batch, absStartPos, absEndPos - absStartPos)
        }

        absStartPos(batch.getAbsoluteCursorPosition(Vec2i(0, end.y), text, font, formattedSize)!!)
        absEndPos(batch.getAbsoluteCursorPosition(end, text, font, formattedSize)!!)
        drawSelectionLine(batch, absStartPos, absEndPos - absStartPos)
    }

    private fun drawSelectionLine(batch: Batch, absStartPos: Vec2c, absSize: Vec2c) = batch.renderBox(
        (absolutePosition + absStartPos).apply { y += 0.2f * font.lineHeight },
        Vec2(absSize).apply { y += font.lineHeight.toFloat() },
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