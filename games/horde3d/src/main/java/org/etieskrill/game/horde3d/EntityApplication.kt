package org.etieskrill.game.horde3d

import org.etieskrill.engine.graphics.model.Model
import org.etieskrill.engine.input.CursorInputAdapter
import org.etieskrill.engine.input.Input
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.input.action.SimpleAction
import org.etieskrill.engine.time.SystemNanoTimePacer
import org.etieskrill.engine.window.window

fun main() {
    val window = window {
        keyInput {
            Input.of(
                Input.bind(Keys.Q).to(SimpleAction { window.shouldClose() })
            )
        }
        cursorInput {
            object : CursorInputAdapter {
            }
        }
    }

    Model.ofFile("vampire.glb", true)

    val pacer = SystemNanoTimePacer((1 / 60).toDouble())
    while (true) {
        window.update(pacer.deltaTimeSeconds)
        pacer.nextFrame()
    }
}