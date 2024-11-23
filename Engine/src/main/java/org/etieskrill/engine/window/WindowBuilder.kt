package org.etieskrill.engine.window

import org.etieskrill.engine.input.CursorInputHandler
import org.etieskrill.engine.input.KeyInputHandler
import org.etieskrill.engine.input.KeyInputManager
import org.etieskrill.engine.window.Window.WindowMode
import org.etieskrill.engine.window.Window.WindowSize
import org.joml.Vector2f
import org.joml.Vector2fc
import org.lwjgl.glfw.GLFW.GLFW_DONT_CARE

class WindowBuilder {
    internal val keyInputs = mutableListOf<KeyInputHandler>()
    internal val cursorInputs = mutableListOf<CursorInputHandler>()
    lateinit var window: Window

    fun keyInput(init: WindowBuilder.() -> KeyInputManager) = keyInputs.add(init())
    fun cursorInput(init: WindowBuilder.() -> CursorInputHandler) = cursorInputs.add(init())
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
        .setKeyInputs(builder.keyInputs)
        .setCursorInputs(builder.cursorInputs)

    val window = javaBuilder.build()
    builder.window = window
    return window
}
