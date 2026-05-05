package org.etieskrill.game.horde3d

import org.etieskrill.engine.graphics.model.Model
import org.etieskrill.engine.input.CursorInputAdapter
import org.etieskrill.engine.input.CursorInputHandler
import org.etieskrill.engine.input.KeyInputHandler
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.input.OverruleGroup
import org.etieskrill.engine.time.SystemNanoTimePacer
import org.etieskrill.engine.window.Window
import kotlin.time.Duration.Companion.seconds

private operator fun Window.plusAssign(cursorInput: CursorInputHandler) = cursorInputs.plusAssign(cursorInput)
private infix fun Window.addCursorInput(cursorInput: CursorInputHandler) = plusAssign(cursorInput)
private operator fun Window.plusAssign(keyInput: KeyInputHandler) = keyInputs.plusAssign(keyInput)

fun main() {
    val window = window {
        inputs {
            keyInput {
                bind { Keys.Q to { window.close() } }
                bind { Keys.W to { delta -> println("Current delta: $delta"); window.close() } }
                Keys.E bindTo { { window.close() } }
                Keys.R bindTo { { delta -> println("Current delta: $delta"); window.close() } }
//                keyInput {} // <- does not compile due to dsl marker :) - so cool
                Keys.E.bindTo(mode = OverruleGroup.Mode.ALL, keys = listOf(Keys.Q)) { { window.close() } }
                Keys.MIDDLE_MOUSE bindTo { { window.close() } }
            }
        }
    }

    window addCursorInput object : CursorInputAdapter {}

    window += object : CursorInputAdapter {}
    window += KeyInputHandler { type, key, action, modifiers -> false }

    Model.ofFile("vampire.glb", true)

    val pacer = SystemNanoTimePacer((1 / 60).toDouble().seconds)
    pacer.start()
    while (!window.isClosing) {
        window.update(pacer.deltaTimeSeconds)
        pacer.nextFrame()
    }
}