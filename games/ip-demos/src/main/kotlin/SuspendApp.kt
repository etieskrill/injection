@file:OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)

package io.github.etieskrill.games.ip.demos.synthwave

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.etieskrill.engine.application.GameApplication
import org.etieskrill.engine.time.TimeResolutionUtils.setSystemTimeResolution
import org.etieskrill.engine.window.Window

//TODO consider local list/channel to put dispatchers, each app will then search for the dispatcher it's current on

fun <T : SuspendApp> runSuspendApp(block: (CoroutineDispatcher) -> T) = runBlocking {
    val uiDispatcher = newSingleThreadContext("UI")
    val app = withContext(uiDispatcher) { block(uiDispatcher) }
    app.runSuspend()
}

abstract class SuspendApp(
    window: Window,
    val uiDispatcher: CoroutineDispatcher,
    val uiScope: CoroutineScope = CoroutineScope(uiDispatcher)
) : GameApplication(window) {

    override fun run() = throw UnsupportedOperationException("Call 'runSuspend' instead")

    suspend fun runSuspend() {
        try {
            //TODO separation: which parts in main and which in window main?
            withContext(uiDispatcher) { init() }
            timer.info("Initialised application")
            _loop()
            while (!window.shouldClose()) delay(100) //TODO expand to multiwindow etc.
        } catch (e: Exception) {
            logger.warn("Caught application exception", e) //TODO better handling and scopes
        } finally {
            withContext(uiDispatcher) { terminate() }
            timer.info("Terminated application")
        }
    }

    override fun _loop() {
        uiScope.launch {
            setSystemTimeResolution(1)
            pacer.start()

            while (!window.shouldClose()) {
                doLoop()
                yield()
                pacer.nextFrame()
            }
        }
    }

}
