package org.etieskrill.engine.window

import org.etieskrill.engine.input.*
import org.etieskrill.engine.input.InputBinding.Trigger
import org.etieskrill.engine.window.Window.WindowMode
import org.etieskrill.engine.window.Window.WindowSize
import org.joml.Vector2f
import org.joml.Vector2fc
import org.lwjgl.glfw.GLFW.GLFW_DONT_CARE

class WindowBuilder {
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
    mode: WindowMode = WindowMode.WINDOWED,
    size: WindowSize = WindowSize.LARGEST_FIT,
    position: Vector2fc = Vector2f(),
    refreshRate: Int = GLFW_DONT_CARE,
    vSync: Boolean = false,
    samples: Int = 0,
    title: String = "Injection Window",
    cursor: Cursor? = Cursor.getDefault(),
    init: WindowBuilder.() -> Unit
): Window {
    val javaBuilder = Window.builder()
        .setMode(mode)
        .setSize(size)
        .setPosition(position as Vector2f)
        .setRefreshRate(refreshRate.toFloat())
        .setVSyncEnabled(vSync)
        .setSamples(samples)
        .setTitle(title)
        .setCursor(cursor)

    val builder = WindowBuilder()
    init(builder)

    javaBuilder
        .setKeyInputs(builder.inputs.flatMap { keyInputBuilders -> keyInputBuilders.keyInputs })
        .setCursorInputs(builder.inputs.flatMap { cursorInputBuilders -> cursorInputBuilders.cursorInputs })

    val window = javaBuilder.build()
    builder.window = window
    return window
}
