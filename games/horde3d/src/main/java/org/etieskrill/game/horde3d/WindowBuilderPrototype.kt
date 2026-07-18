package org.etieskrill.game.horde3d

import org.etieskrill.engine.graphics.model.Model
import org.etieskrill.engine.input.CursorInputAdapter
import org.etieskrill.engine.input.CursorInputHandler
import org.etieskrill.engine.input.KeyInputHandler
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.OverruleGroup
import org.etieskrill.engine.time.LoopPacer
import org.etieskrill.engine.window.Window
import kotlin.time.Duration.Companion.seconds

private operator fun Window.plusAssign(cursorInput: CursorInputHandler) = cursorInputs.plusAssign(cursorInput)
private infix fun Window.addCursorInput(cursorInput: CursorInputHandler) = plusAssign(cursorInput)
private operator fun Window.plusAssign(keyInput: KeyInputHandler) = keyInputs.plusAssign(keyInput)

fun main() {
    val window = window {
        inputs {
            keyInput {
                bind { Key.Q to { window.close() } }
                bind { Key.W to { delta -> println("Current delta: $delta"); window.close() } }
                Key.E bindTo { { window.close() } }
                Key.R bindTo { { delta -> println("Current delta: $delta"); window.close() } }
//                keyInput {} // <- does not compile due to dsl marker :) - so cool
                Key.E.bindTo(mode = OverruleGroup.Mode.ALL, keys = listOf(Key.Q)) { { window.close() } }
                Key.MIDDLE_MOUSE bindTo { { window.close() } }
            }
        }
    }

    window addCursorInput object : CursorInputAdapter {}

    window += object : CursorInputAdapter {}
    window += KeyInputHandler { type, key, action, modifiers -> false }

    Model.ofFile("vampire.glb", true)

    val pacer = LoopPacer((1 / 60).toDouble().seconds)
    pacer.start()
    while (!window.isClosing) {
        window.update(pacer.deltaTimeSeconds)
        pacer.nextFrame()
    }
}