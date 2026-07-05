package org.etieskrill.engine.audio

import org.etieskrill.engine.common.Disposable
import org.lwjgl.openal.AL
import org.lwjgl.openal.ALC10
import org.lwjgl.openal.ALCapabilities

internal class AudioContext(
    val handle: Long,
    val caps: ALCapabilities,
    val listener: AudioListener
) : Disposable {
    companion object {
        fun create(device: AudioDevice): AudioContext {
            val alContext = ALC10.alcCreateContext(device.handle, null as IntArray?)
            ALC10.alcMakeContextCurrent(alContext)

            val context = AudioContext(
                alContext,
                AL.createCapabilities(device.caps), //TODO tdl contexts
                AudioListener()
            )
            checkContextError(context.handle, "Failed to create audio context")
            return context
        }
    }

    override fun dispose() = ALC10.alcDestroyContext(handle)
}