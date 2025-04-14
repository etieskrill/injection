package org.etieskrill.engine.window

import org.etieskrill.engine.input.CursorInputHandler
import org.etieskrill.engine.input.Input
import org.etieskrill.engine.input.InputBinding
import org.etieskrill.engine.input.InputBinding.Trigger
import org.etieskrill.engine.input.KeyInputHandler
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.input.OverruleGroup
import org.etieskrill.engine.window.Window.WindowMode
import org.etieskrill.engine.window.Window.WindowSize
import org.joml.Vector2fc
import org.lwjgl.glfw.GLFW.GLFW_DONT_CARE

class WindowBuilder(
    var mode: WindowMode = WindowMode.WINDOWED,
    var size: WindowSize = WindowSize.DEFAULT,
    var position: Vector2fc? = null,
    var refreshRate: Int = GLFW_DONT_CARE,
    var vSync: Boolean = false,
    var samples: Int = 0,
    var resizeable: Boolean = false,
    var title: String = "Injection Window",
    var cursor: Cursor? = Cursor.getDefault(),
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

    val window = Window.builder()
        .setMode(builder.mode)
        .setSize(builder.size)
        .setPosition(builder.position)
        .setRefreshRate(builder.refreshRate.toFloat())
        .setVSyncEnabled(builder.vSync)
        .setSamples(builder.samples)
        .setResizeable(builder.resizeable)
        .setTitle(builder.title)
        .setCursor(builder.cursor)
        .setKeyInputs(builder.inputs.flatMap { keyInputBuilders -> keyInputBuilders.keyInputs })
        .setCursorInputs(builder.inputs.flatMap { cursorInputBuilders -> cursorInputBuilders.cursorInputs })
        .setCreateHidden(builder.createHidden)
        .setTransparency(builder.transparency)
        .build()

    builder.window = window
    return window
}
