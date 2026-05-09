package org.etieskrill.game.horde3d

import org.etieskrill.engine.input.CursorInputHandler
import org.etieskrill.engine.input.Input
import org.etieskrill.engine.input.InputBinding
import org.etieskrill.engine.input.InputBinding.Trigger
import org.etieskrill.engine.input.KeyInputHandler
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.input.OverruleGroup
import org.etieskrill.engine.window.Cursor
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.Window.WindowMode
import org.etieskrill.engine.window.Window.WindowSize
import org.joml.Vector2fc
import org.joml.Vector2i
import org.joml.Vector2ic

class WindowBuilder(
    var mode: WindowMode = WindowMode.WINDOWED,
    var size: Vector2ic = WindowSize.DEFAULT,
    var position: Vector2fc? = null,
    var refreshRate: Int = -1,
    var vSync: Boolean = false,
    var samples: Int = 0,
    var resizeable: Boolean = true,
    var title: String = "Injection Window",
    var cursor: Cursor? = Cursor(),
    var createHidden: Boolean = false,
    var transparency: Boolean = false
) {
    lateinit var window: Window

    internal val inputs = mutableListOf<InputBuilder>()

    fun inputs(init: InputBuilder.() -> Unit) {
        val inputBlock = InputBuilder()
        init(inputBlock)
        inputs.add(inputBlock)
    }
}

@DslMarker
annotation class WindowInputMarker

@WindowInputMarker //FIXME is there a better way to exclude certain properties from dsl without an intermediate receiver?
class InputBuilder {
    internal val keyInputs = mutableListOf<KeyInputHandler>()
    internal val cursorInputs = mutableListOf<CursorInputHandler>()

    fun keyInput(init: InputBuilderBlock.() -> Unit) {
        val builder = InputBuilderBlock()
        init(builder)
        keyInputs.add(Input.of(*builder.inputs.toTypedArray()))
    }

    fun keyInputHandler(init: InputBuilderBlock.() -> KeyInputHandler) = keyInputs.add(init(InputBuilderBlock()))
    fun cursorInputHandler(init: InputBuilderBlock.() -> CursorInputHandler) =
        cursorInputs.add(init(InputBuilderBlock()))
}

@WindowInputMarker
class InputBuilderBlock {
    internal val inputs = mutableListOf<InputBinding>()

    fun bind(init: InputBuilderBlock.() -> Pair<Keys, (Double) -> Unit>) {
        val (key, action) = init()
        inputs.add(Input.bind(key).to(action))
    }

    infix fun Keys.bindTo(action: Keys.() -> (Double) -> Unit) {
        inputs.add(Input.bind(this).to { delta -> action.invoke(this).invoke(delta) })
    }

    fun Keys.bindTo(
        trigger: Trigger = Trigger.ON_PRESS,
        mode: OverruleGroup.Mode?,
        keys: List<Keys>?,
        action: Keys.() -> (Double) -> Unit
    ) {
        inputs.add(
            Input.bind(this).on(trigger).group(mode, *keys?.toTypedArray()!!)
                .to { delta -> action.invoke(this).invoke(delta) })
    }
}

fun window(
    init: WindowBuilder.() -> Unit = {}
): Window {
    val builder = WindowBuilder()
    init(builder)

    val window = Window(
        mode = builder.mode,
        size = builder.size,
        position = builder.position?.let { Vector2i(it.x().toInt(), it.y().toInt()) },
        refreshRate = builder.refreshRate.toUInt(),
        vSync = builder.vSync,
        samples = builder.samples.toUInt(),
        resizeable = builder.resizeable,
        title = builder.title,
        cursor = builder.cursor ?: Cursor(),
        createHidden = builder.createHidden,
        transparency = builder.transparency
    )
    builder.inputs.flatMap { it.keyInputs }.forEach { window.keyInputs += it }
    builder.inputs.flatMap { it.cursorInputs }.forEach { window.cursorInputs += it }

    builder.window = window
    return window
}
