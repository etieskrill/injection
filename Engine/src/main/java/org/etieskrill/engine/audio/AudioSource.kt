package org.etieskrill.engine.audio

import org.etieskrill.engine.common.Disposable
import org.joml.Vector3f
import org.joml.Vector3fc
import org.lwjgl.openal.AL10.AL_GAIN
import org.lwjgl.openal.AL10.AL_PLAYING
import org.lwjgl.openal.AL10.AL_POSITION
import org.lwjgl.openal.AL10.AL_SOURCE_STATE
import org.lwjgl.openal.AL10.alDeleteSources
import org.lwjgl.openal.AL10.alGetSourcef
import org.lwjgl.openal.AL10.alGetSourcei
import org.lwjgl.openal.AL10.alSource3f
import org.lwjgl.openal.AL10.alSourcePause
import org.lwjgl.openal.AL10.alSourcePlay
import org.lwjgl.openal.AL10.alSourceStop
import org.lwjgl.openal.AL10.alSourcef
import org.lwjgl.openal.AL10.alSourcei
import org.lwjgl.openal.AL11.AL_SAMPLE_OFFSET
import org.lwjgl.openal.AL11.AL_SEC_OFFSET
import java.nio.ShortBuffer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class AudioSource internal constructor(
    internal val handle: Int,
    val sampleRate: Int,
    val numSamples: Int,
    val buffer: ShortBuffer?,
    val duration: Duration = (numSamples / sampleRate).seconds,
    private var disposed: Boolean = false
) : Disposable {

    val isPlaying: Boolean = checkNotDisposed { alGetSourcei(handle, AL_SOURCE_STATE) == AL_PLAYING }

    fun play() = checkNotDisposed { alSourcePlay(handle) }

    fun pause() = checkNotDisposed { alSourcePause(handle) }

    fun stop() = checkNotDisposed { alSourceStop(handle) }

    /**
     * The current offset into the bound buffer in samples.
     */
    var offsetSamples: Int
        get() = checkNotDisposed { alGetSourcei(handle, AL_SAMPLE_OFFSET) }
        set(value) {
            checkNotDisposed {}
            require(value >= 0) { "Offset must not be negative" }
            require(value < numSamples) { "Offset must be smaller than the number of samples" }

            alSourcei(handle, AL_SAMPLE_OFFSET, value)
        }

    /**
     * The current offset into the bound buffer in seconds.
     */
    var offsetSeconds: Float
        get() = checkNotDisposed { alGetSourcef(handle, AL_SEC_OFFSET) }
        set(value) {
            checkNotDisposed {}
            require(value >= 0) { "Offset must not be negative" }
            require(value * sampleRate < numSamples) { "Offset must be smaller than the duration" }

            alSourcef(handle, AL_SEC_OFFSET, value)
        }

    var gain: Float
        get() = checkNotDisposed { alGetSourcef(handle, AL_GAIN) }
        set(value) {
            checkNotDisposed {}
            require(value >= 0) { "Gain must be positive" }
            alSourcef(handle, AL_GAIN, value)
        }

    override fun dispose() {
        alDeleteSources(handle)
        //FIXME the buffers are in the goddamn walls (even the heap buffer copies are not cleaned up grrrr)
        disposed = true
    }

    private fun <T> checkNotDisposed(block: () -> T): T {
        check(!disposed) { "Audio source was already disposed" }
        return block()
    }

}

class MonoAudioSource internal constructor(handle: Int, sampleRate: Int, numSamples: Int, buffer: ShortBuffer?) :
    AudioSource(handle, sampleRate, numSamples, buffer) {

    var position: Vector3fc = Vector3f(0f)
        set(value) {
            field = value
            alSource3f(handle, AL_POSITION, value.x(), value.y(), value.z())
        }

}

class StereoAudioSource internal constructor(handle: Int, sampleRate: Int, numSamples: Int, buffer: ShortBuffer?) :
    AudioSource(handle, sampleRate, numSamples, buffer)
