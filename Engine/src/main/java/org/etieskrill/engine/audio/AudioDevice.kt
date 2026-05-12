package org.etieskrill.engine.audio

import org.etieskrill.engine.common.Disposable
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.*
import org.lwjgl.openal.ALCCapabilities

class AudioDevice private constructor(
    val name: String,
    internal val handle: Long,
    internal val caps: ALCCapabilities
) : Disposable {
    companion object {
        fun create(name: String? = null): AudioDevice {
            val handle = alcOpenDevice(name)
            checkContextError(handle, "Failed to open audio device")
            val deviceName = name ?: alcGetString(handle, ALC_DEVICE_SPECIFIER)!!

            val caps = ALC.createCapabilities(handle)
            return AudioDevice(deviceName, handle, caps)
        }
    }

    override fun dispose() {
        check(alcCloseDevice(handle))
        { "Failed to dispose OpenAL device $name: there may still be contexts and/or buffers present" }
    }
}

