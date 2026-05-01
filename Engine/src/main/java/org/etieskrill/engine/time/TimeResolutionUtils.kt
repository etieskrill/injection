package org.etieskrill.engine.time

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.system.JNI
import org.lwjgl.system.Platform
import org.lwjgl.system.Platform.LINUX
import org.lwjgl.system.Platform.WINDOWS
import org.lwjgl.system.windows.WindowsLibrary
import kotlin.time.Duration

val logger = KotlinLogging.logger {}

fun setSystemTimeResolution(resolution: Duration) = when (val platform = Platform.get()) {
    WINDOWS -> {
        val ret = WindowsLibrary("winmm.dll").use {
            val beginFunctionPointer = it.getFunctionAddress("timeBeginPeriod")
            JNI.invokeI(resolution.inWholeMilliseconds.toInt(), beginFunctionPointer)
        }

        if (ret != 0) logger.warn { "Unsuccessfully tried to set system time resolution to $resolution, return value: $ret" }
        else logger.debug { "Successfully set system time resolution to $resolution" }
    }

    LINUX -> {
        //could try to check res with clock_getres from time.h ... however one may load libs for linux
        //the standard resolution appears to be 1000 hz according to the internet,
        //and 1 ns according to me (a c program i wrote using the aforementioned function)
        //the fact that even in wsl i get several hundreds of FPS seems to support the former theory however
    }

    else -> throw UnsupportedOperationException("Unsupported platform: $platform")
}

fun resetSystemTimeResolution(resolution: Duration) = when (val platform = Platform.get()) {
    WINDOWS -> {
        val ret = WindowsLibrary("winmm.dll").use {
            val endFunctionPointer = it.getFunctionAddress("timeEndPeriod")
            JNI.invokeI(resolution.inWholeMilliseconds.toInt(), endFunctionPointer)
        }

        if (ret != 0) logger.warn { "Unsuccessfully tried to reset system time resolution, return value: $ret" }
        else logger.debug { "Successfully reset system time resolution" }
    }

    LINUX -> {}

    else -> throw UnsupportedOperationException("Unsupported platform: $platform")
}
