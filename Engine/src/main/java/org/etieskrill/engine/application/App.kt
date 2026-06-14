package org.etieskrill.engine.application

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import org.etieskrill.engine.audio.Audio
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.config.InjectionConfig
import org.etieskrill.engine.entity.system.EntitySystem
import org.etieskrill.engine.graphics.gl.renderer.GLRenderer
import org.etieskrill.engine.graphics.text.TrueTypeFont
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.KeyInputHandler
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.time.StepTimer
import org.etieskrill.engine.time.SystemNanoTimePacer
import org.etieskrill.engine.time.resetSystemTimeResolution
import org.etieskrill.engine.time.setSystemTimeResolution
import org.etieskrill.engine.util.FixedArrayDeque
import org.etieskrill.engine.util.disposeDefaultLoaders
import org.etieskrill.engine.window.Window
import org.lwjgl.glfw.GLFW.glfwTerminate
import org.lwjgl.opengl.GL.destroy
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.measureTime

private val logger = KotlinLogging.logger {}

abstract class App(
    protected val window: Window = Window()
) : Disposable {

    protected val pacer = SystemNanoTimePacer((1.0 / window.refreshRate.toDouble()).seconds)

    protected val renderer = GLRenderer()

    protected val entitySystem = EntitySystem()

    protected var avgCpuTime = 0.0; private set
    private val cpuTimes = FixedArrayDeque<Double>(window.refreshRate.toInt())

    protected val timer = StepTimer()

    protected open val uiDispatcher: CoroutineDispatcher = ImmediateDispatcher.limitedParallelism(1)
    protected open val uiScope = CoroutineScope(uiDispatcher)

    fun uiDo(block: suspend CoroutineScope.() -> Unit) = uiScope.launch(block = block)

    object ImmediateDispatcher : CoroutineDispatcher() { //FIXME why it not immediate tho
        override fun dispatch(context: CoroutineContext, block: Runnable) = block.run()
    }

    companion object {
        init {
            InjectionConfig.init()
            logger.info { "Loaded static application configuration" }
        }
    }

    init {
        timer.start()

        window.keyInputs += KeyInputHandler { type, key, _, modifiers ->
            if (type != Key.Type.KEYBOARD) return@KeyInputHandler false
            if (key == Keys.ESC.glfwKey && modifiers == Keys.Mod.SHIFT.glfwKey
                || key == Keys.W.glfwKey && modifiers == Keys.Mod.CONTROL.glfwKey
            ) {
                window.isClosing = true
                return@KeyInputHandler true
            }

            false
        }

        window.uiScope = uiScope

        timer.info { "Initialised window configuration" }
    }

    open fun run() {
        try {
            init()
            timer.info { "Initialised application" }
            internalLoop()
        } catch (e: Exception) {
            logger.error(e) { "Caught app exception" }
        } finally {
            terminate()
            timer.info { "Terminated application" }
        }
    }

    protected open fun init() {}

    protected open fun internalLoop() {
        setSystemTimeResolution(SYSTEM_TIME_RESOLUTION_MILLIS.milliseconds)
        pacer.start()
        while (!window.isClosing) {
            update()
            pacer.nextFrame()
        }
    }

    protected fun update() {
        window.screenBuffer.clear()
        window.screenBuffer.bind()
        renderer.nextFrame()

        val cpuTime = measureTime {
            val delta = pacer.deltaTimeSeconds
            loop(delta)
            entitySystem.update(delta)
            render()
            window.update(delta)
        }

        cpuTimes.add(cpuTime.toDouble(DurationUnit.SECONDS))
        avgCpuTime = cpuTimes.average()
    }

    protected abstract fun loop(delta: Double)

    protected open fun render() {}

    protected fun terminate() {
        dispose()
        resetSystemTimeResolution(SYSTEM_TIME_RESOLUTION_MILLIS.milliseconds)
        window.isClosing = true
        window.dispose()
        disposeDefaultLoaders()
        TrueTypeFont.disposeLibrary()
        destroy()
        glfwTerminate()
        Audio.dispose()
    }

    override fun dispose() = Unit

}
