package org.etieskrill.engine.audio

import org.etieskrill.engine.Disposable
import org.etieskrill.engine.config.ResourcePaths.AUDIO_PATH
import org.etieskrill.engine.util.ResourceReader
import org.joml.Vector3f
import org.joml.Vector3fc
import org.lwjgl.BufferUtils.*
import org.lwjgl.openal.AL
import org.lwjgl.openal.AL11.*
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.*
import org.lwjgl.openal.ALCCapabilities
import org.lwjgl.openal.ALCapabilities
import org.lwjgl.stb.STBVorbis.*
import org.lwjgl.stb.STBVorbisInfo
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.Char.Companion.MIN_VALUE as nullChar

private val logger = LoggerFactory.getLogger(Audio::class.java)

class AudioDevice private constructor(val name: String, internal val handle: Long, internal val caps: ALCCapabilities) :
    Disposable {
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

private class AudioContext(val handle: Long, val caps: ALCapabilities, val listener: AudioListener) : Disposable {
    companion object {
        fun create(device: AudioDevice): AudioContext {
            val alContext = alcCreateContext(device.handle, null as IntArray?)
            alcMakeContextCurrent(alContext)

            val context = AudioContext(
                alContext,
                AL.createCapabilities(device.caps), //TODO tdl contexts
                AudioListener()
            )
            checkContextError(context.handle, "Failed to create audio context")
            return context
        }
    }

    override fun dispose() = alcDestroyContext(handle)
}

private typealias AudioBuffer = Int

abstract class AudioSource internal constructor(
    internal val handle: Int,
    val sampleRate: Int,
    val numSamples: Int,
    val buffer: ShortBuffer?,
    val duration: Duration = (numSamples / sampleRate).seconds
) : Disposable {

    fun play() = alSourcePlay(handle)

    fun pause() = alSourcePause(handle)

    fun stop() = alSourceStop(handle)

    /**
     * The current offset into the bound buffer in samples.
     */
    var offsetSamples: Int
        get() = alGetSourcei(handle, AL_SAMPLE_OFFSET)
        set(value) {
            require(value >= 0) { "Offset must not be negative" }
            require(value < numSamples) { "Offset must be smaller than the number of samples" }

            alSourcei(handle, AL_SAMPLE_OFFSET, value)
        }

    /**
     * The current offset into the bound buffer in seconds.
     */
    var offsetSeconds: Float
        get() = alGetSourcef(handle, AL_SEC_OFFSET)
        set(value) {
            require(value >= 0) { "Offset must not be negative" }
            require(value * sampleRate < numSamples) { "Offset must be smaller than the duration" }

            alSourcef(handle, AL_SEC_OFFSET, value)
        }

    var gain: Float
        get() = alGetSourcef(handle, AL_GAIN)
        set(value) {
            require(gain >= 0) { "Gain must be positive" }
            alSourcef(handle, AL_GAIN, value)
        }

    override fun dispose() = alDeleteSources(handle)

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

class AudioListener {
    var position: Vector3fc = Vector3f(0f)
        set(value) {
            field = value
            alListener3f(AL_POSITION, value.x(), value.y(), value.z())
        }

    var direction: Vector3fc = Vector3f(0f)
        set(value) {
            field = value
            setOrientation(direction, up)
        }

    var up: Vector3fc = Vector3f(0f, 1f, 0f)
        set(value) {
            field = value
            setOrientation(direction, up)
        }

    private val orientationBuffer = createFloatBuffer(6)
    private fun setOrientation(direction: Vector3fc, up: Vector3fc) {
        orientationBuffer.position(0)
        direction.get(orientationBuffer).position(3)
        up.get(orientationBuffer)
        alListenerfv(AL_ORIENTATION, orientationBuffer.rewind())
    }
}

object Audio : Disposable {
    fun getAudioDevices(): List<String> {
        if (!alcIsExtensionPresent(0, "ALC_ENUMERATION_EXTENSION")) {
            logger.info("Audio devices could not be enumerated: returning default")
            return listOf(alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER)!!)
        }

        val devices = alcGetString(0, ALC_DEVICE_SPECIFIER)!!.split(nullChar)
        logger.info("Available audio devices: $devices")
        return devices
    }

    private val sources = mutableMapOf<AudioContext, MutableList<AudioSource>>()
    private val buffers = mutableMapOf<AudioContext, MutableList<AudioBuffer>>()
    private lateinit var currentDevice: AudioDevice
    private val devices: MutableList<AudioDevice> = mutableListOf()
    private lateinit var currentContext: AudioContext
    private val defaultContexts: MutableMap<AudioDevice, AudioContext> = mutableMapOf()

    fun useAudioDevice(device: AudioDevice) {
        currentDevice = device
        if (currentDevice !in devices) devices.add(currentDevice)
        currentContext = defaultContexts.getOrPut(device) { AudioContext.create(device) }
        alcMakeContextCurrent(currentContext.handle)
    }

    val listener: AudioListener
        get() {
            checkInit()
            return currentContext.listener
        }

    fun readMono(path: String): MonoAudioSource = read(path, AudioMode.MONO) as MonoAudioSource
    fun readStereo(path: String): StereoAudioSource = read(path, AudioMode.STEREO) as StereoAudioSource
    fun read(path: String, mode: AudioMode = AudioMode.DONT_CARE, retainBuffer: Boolean = false): AudioSource {
        checkInit()

        require(path.endsWith(".ogg")) { "Can only parse OGG sound files" }
        val encodedAudio = ResourceReader.getRawResource(AUDIO_PATH + path)
        val pcmAudio = vorbisDecodeToPCM(encodedAudio, mode)
        check(pcmAudio.mode != AudioMode.DONT_CARE)

        val buffer = alGenBuffers()

        alBufferData(buffer, pcmAudio.mode.al, pcmAudio.buffer, pcmAudio.sampleRate)
        checkError("Failed to buffer audio data")

        val source = alGenSources()
        checkError("Failed to create source")

        alSourcei(source, AL_BUFFER, buffer)
        checkError("Failed to bind buffer to source")

        val audioSource = when (pcmAudio.mode) {
            AudioMode.MONO -> MonoAudioSource(
                source,
                pcmAudio.sampleRate,
                pcmAudio.numSamples,
                pcmAudio.buffer.takeIf { retainBuffer }
            )

            AudioMode.STEREO -> StereoAudioSource(
                source,
                pcmAudio.sampleRate,
                pcmAudio.numSamples,
                pcmAudio.buffer.takeIf { retainBuffer }
            )

            else -> error("uh oh")
        }

        sources.getOrPut(currentContext) { mutableListOf() } += audioSource
        buffers.getOrPut(currentContext) { mutableListOf() } += buffer

        return audioSource
    }

    private fun checkInit() {
        if (!::currentDevice.isInitialized) {
            currentDevice = AudioDevice.create()
            devices += currentDevice
        }
        if (!::currentContext.isInitialized) {
            currentContext = AudioContext.create(currentDevice)
            defaultContexts[currentDevice] = currentContext
            alcMakeContextCurrent(currentContext.handle)
        }
    }

    override fun dispose() {
        sources.forEach { (context, sources) ->
            //TODO save context in objects and use direct methods
            alDeleteSourcesDirect(context.handle, sources.map { it.handle }.toIntArray())
        }
        sources.clear()

        buffers.forEach { (context, buffers) ->
            alDeleteBuffersDirect(context.handle, buffers.toIntArray())
        }
        buffers.clear()

        alcMakeContextCurrent(0)

        defaultContexts.values.forEach { it.dispose() }
        defaultContexts.clear()

        devices.forEach { it.dispose() }
        devices.clear()
    }
}

enum class AudioMode(internal val al: Int = 0) { DONT_CARE, MONO(AL_FORMAT_MONO16), STEREO(AL_FORMAT_STEREO16) }

data class PCMAudio(
    val sampleRate: Int,
    val numSamples: Int,
    val mode: AudioMode,
    val buffer: ShortBuffer
)

private fun vorbisDecodeToPCM(
    encodedAudio: ByteBuffer,
    audioMode: AudioMode = AudioMode.DONT_CARE,
): PCMAudio {
    val error = createIntBuffer(1)
    val decoder = stb_vorbis_open_memory(encodedAudio, error, null)
    if (decoder == 0L) error("Failed to open audio file. Vorbis error: ${error.get()}")

    var channels: Int
    var sampleRate: Int

    STBVorbisInfo.malloc().use { info ->
        stb_vorbis_get_info(decoder, info)
        channels = info.channels()
        sampleRate = info.sample_rate()
    }

    val numSamples = stb_vorbis_stream_length_in_samples(decoder)
    val mode = when (audioMode) { //this is fucking marvelous, and you cannot tell me otherwise
        AudioMode.DONT_CARE -> {
            if (channels == 1) AudioMode.MONO else AudioMode.STEREO
        }

        AudioMode.MONO -> {
            channels = 1
            AudioMode.MONO
        }

        AudioMode.STEREO -> {
            channels = 2
            AudioMode.STEREO
        }
    }
    val buffer = vorbisLoad(decoder, channels, numSamples, mode)

    stb_vorbis_close(decoder)

    return PCMAudio(sampleRate, numSamples, mode, buffer)
}

private fun vorbisLoad(decoder: Long, channels: Int, numSamples: Int, mode: AudioMode) = when (mode) {
    AudioMode.MONO -> vorbisLoadMono(decoder, channels, numSamples)
    AudioMode.STEREO -> vorbisLoadStereo(decoder, channels, numSamples)
    else -> error("waaagh")
}

private fun vorbisLoadMono(decoder: Long, channels: Int, numSamples: Int): ShortBuffer {
    val buffer = createShortBuffer(numSamples)
    val buffers = listOf(buffer) + generateSequence { createShortBuffer(numSamples) }.take(channels).toList()
    val pointers = createPointerBuffer(buffers.size)
    buffers.forEach { pointers.put(it) }
    pointers.rewind()

    stb_vorbis_get_samples_short(decoder, pointers, numSamples)
    return buffer
}

private fun vorbisLoadStereo(decoder: Long, channels: Int, numSamples: Int): ShortBuffer {
    require(channels == 2) { "Stereo audio source must have two channels" }

    val buffer = createShortBuffer(numSamples * channels)
    stb_vorbis_get_samples_short_interleaved(decoder, channels, buffer)
    return buffer
}

private fun checkError(message: String = "OpenAL error occurred") {
    val error = alGetError()
    val cause = when (error) {
        AL_INVALID_NAME -> "invalid name"
        AL_INVALID_ENUM -> "invalid enum"
        AL_INVALID_VALUE -> "invalid value"
        AL_INVALID_OPERATION -> "invalid operation"
        AL_OUT_OF_MEMORY -> "out of memory"
        AL_NO_ERROR -> ""
        else -> error("Unknown OpenAL error: $error")
    }
    check(error == AL_NO_ERROR) { "$message: $cause" }
}

private fun checkContextError(device: Long, message: String = "OpenALC error occurred") {
    val error = alcGetError(device)
    val cause = when (error) {
        ALC_INVALID_DEVICE -> "invalid device"
        ALC_INVALID_CONTEXT -> "invalid context"
        ALC_INVALID_ENUM -> "invalid enum"
        ALC_INVALID_VALUE -> "invalid value"
        ALC_OUT_OF_MEMORY -> "out of memory"
        ALC_NO_ERROR -> ""
        else -> error("Unknown OpenALC error: $error")
    }
    check(error == ALC_NO_ERROR) { "$message: $cause" }
}
