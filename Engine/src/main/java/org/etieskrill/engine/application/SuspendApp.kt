@file:OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)

package org.etieskrill.engine.application

import io.github.oshai.kotlinlogging.KotlinLogging
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
import org.etieskrill.engine.time.setSystemTimeResolution
import org.etieskrill.engine.window.Window
import kotlin.time.Duration.Companion.milliseconds

//TODO consider local list/channel to put dispatchers, each app will then search for the dispatcher it's current on
private var dispatcher: CoroutineDispatcher? = null
private var firstApp = true

private val logger = KotlinLogging.logger {}

fun <T : SuspendApp> runSuspendApp(block: () -> T) = runBlocking {
    if (!firstApp) TODO("Can only launch one app+window for now")
    else firstApp = false

    val uiDispatcher = newSingleThreadContext("UI")
    dispatcher = uiDispatcher
    val app = withContext(uiDispatcher) { block() }
    app.runSuspend()
}

abstract class SuspendApp(window: Window) : App(window) {

    override val uiDispatcher = dispatcher!!
    override val uiScope = CoroutineScope(uiDispatcher)

    override fun run() = throw UnsupportedOperationException("Call 'runSuspend' instead")

    init {
        window.setUiScope(uiScope) //juuust a little bit disgusting
    }

    suspend fun runSuspend() {
        try {
            //TODO separation: which parts in main and which in window main?
            withContext(uiDispatcher) { init() }
            timer.info("Initialised application")
            internalLoop()
            while (!window.shouldClose()) delay(100) //TODO expand to multiwindow etc.
        } catch (e: Exception) {
            logger.error(e) { "Caught application exception" } //TODO better handling and scopes
        } finally {
            withContext(uiDispatcher) { terminate() }
            timer.info("Terminated application")
        }
    }

    override fun internalLoop() {
        uiScope.launch {
            setSystemTimeResolution(1.milliseconds)
            pacer.start()

            while (!window.shouldClose()) {
                update()
                yield()
                pacer.nextFrame()
            }
        }
    }

}
