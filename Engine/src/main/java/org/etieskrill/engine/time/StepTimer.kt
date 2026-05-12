package org.etieskrill.engine.time

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger {}

class StepTimer(
    val targetLogger: KLogger = logger,
) {

    private lateinit var time: kotlin.time.TimeMark

    fun start() {
        time = TimeSource.Monotonic.markNow()
    }

    fun log(message: () -> String) = debug(message)
    fun info(message: () -> String) = targetLogger.info { "${message()} [${getTimeFormatted()}s]" }
    fun debug(message: () -> String) = targetLogger.debug { "${message()} [${getTimeFormatted()}s]" }
    fun trace(message: () -> String) = targetLogger.trace { "${message()} [${getTimeFormatted()}s]" }

    fun getTimeFormatted(): String {
        return "%.3f".format(time.elapsedNow().toDouble(DurationUnit.SECONDS))
    }

}
