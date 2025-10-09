package org.etieskrill.engine.graphics.text

import io.github.oshai.kotlinlogging.KotlinLogging
import org.joml.Vector2i
import org.joml.Vector2ic

private val logger = KotlinLogging.logger {}

class TextEditor(font: Font) {

    var text: String
        get() = toString()
        set(value) = TODO()
    private val field = TextEditorField()
    var font: Font = font
        get() = field
        set(value) {
            field = value
        }
    var size: Vector2i? = null

    fun write(c: Char) {
        when (c) {
            '\n' -> {
                field[cursor.position] += '\n'

                val line = field.text[cursor.position.y]
                val beforeCursor = line.take(cursor.position.x + 1)
                field.text[cursor.position.y] = beforeCursor.toMutableList()
                val behindCursor = line.drop(cursor.position.x + 1)
                field.text.add(cursor.position.y + 1, behindCursor.toMutableList())

                update()
                cursor.home()
                cursor.down()
            }

            else -> {
                field[cursor.position] += c
                update()
                cursor.right()
            }
        }
    }

    operator fun plusAssign(c: Char) = write(c)

    fun remove(ctrl: Boolean = false) {
        if (ctrl) TODO()

        if (field[cursor.position].isEmpty()) {
            cursor.up()
            cursor.end()
            field.remove(cursor.position)
            cursor.left()
        } else {
            field.remove(cursor.position)
            cursor.left()
        }
        update()
    }

    fun copy(): String = TODO()
    fun cut(): String = TODO()
    fun paste(s: String): Unit = TODO()

    val cursor = Cursor()

    inner class Cursor {
        var position = Vector2i()

        fun up(select: Boolean = false) = position.apply {
            x = kotlin.math.min(x, if (field.text.size > y - 1 && y - 1 >= 0) field.text[y - 1].size - 1 else 0)
            y = kotlin.math.max(0, y - 1)
        }

        fun down(select: Boolean = false) = position.apply {
            x = kotlin.math.min(x, if (field.text.size > y + 1) field.text[y + 1].size else 0)
            y = kotlin.math.min(y + 1, field.text.size - 1)
            println("cursor pos: $position")
        }

        fun left(ctrl: Boolean = false, select: Boolean = false) = position.apply {
            if (position.y > 0 && x - 1 < 0) {
                up()
                end()
            } else {
                x = kotlin.math.max(0, x - 1)
            }
        }

        fun right(ctrl: Boolean = false, select: Boolean = false) = position.apply {
            if (field.text.size - 1 > position.y && x + 1 > field.text[position.y].count { it != '\n' }) { //TODO replace with isPrintable or w/e
                down()
                home()
            } else {
                val lineLength = if (field.text.size > position.y) field.text[position.y].size else 0
                x = kotlin.math.min(x + 1, lineLength)
            }
        }

        fun home(ctrl: Boolean = false, select: Boolean = false) = position.apply { x = 0 }
        fun end(ctrl: Boolean = false, select: Boolean = false) = position.apply {
            x = if (field.text.size > position.y) field.text[position.y].count { it != '\n' } else 0
        }
    }

    private val _gridSize = listOf<Int>()
    private val _wrappedGridSize = listOf<List<Int>>()
    val gridSize: List<Int>
        get() = if (size == null) _gridSize else _wrappedGridSize.flatten()

    val selectedPosition: Vector2ic = Vector2i()
    val selectedSize: Vector2ic = Vector2i()

    val numPrintableChars: Int
        get() = TODO()

    //adjusts _gridSize, _wrappedGridSize, checks if cursor and selection have valid state
    private fun update() {
        var pen = Vector2i()
    }

    override fun toString() = field.toString()

    private class TextEditorField {
        val text = mutableListOf<MutableList<Char>>()

        fun remove(position: Vector2i) {
            text.takeIf { it.size <= position.y }
                ?.add(mutableListOf())

            text[position.y]
                .takeIf { it.isNotEmpty() }
                ?.removeAt(position.x - 1)
        }

        operator fun get(position: Vector2i) = Accessor(position)

        inner class Accessor(val position: Vector2i) {
            val size get() = if (text.size > position.y) text[position.y].size else 0

            fun isEmpty() = if (text.size > position.y) text[position.y].isEmpty() else true
            fun isNotEmpty() = !isEmpty()

            operator fun plusAssign(c: Char) {
                if (text.size <= position.y) text.add(mutableListOf())
                text[position.y].apply {
                    if (isEmpty()) add(c)
                    else add(position.x, c)
                }
            }
        }

        fun toEscapedString() = text.joinToString("") { it.joinToString("").replace("\n", "\\n") }
        override fun toString() = text.joinToString("") { it.joinToString("") }
    }

}