package org.etieskrill.engine.audio

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.util.ResourceReader
import org.lwjgl.BufferUtils.createIntBuffer
import org.lwjgl.BufferUtils.createPointerBuffer
import org.lwjgl.BufferUtils.createShortBuffer
import org.lwjgl.openal.AL10.AL_BUFFER
import org.lwjgl.openal.AL10.AL_FORMAT_MONO16
import org.lwjgl.openal.AL10.AL_FORMAT_STEREO16
import org.lwjgl.openal.AL10.AL_INVALID_ENUM
import org.lwjgl.openal.AL10.AL_INVALID_NAME
import org.lwjgl.openal.AL10.AL_INVALID_OPERATION
import org.lwjgl.openal.AL10.AL_INVALID_VALUE
import org.lwjgl.openal.AL10.AL_NO_ERROR
import org.lwjgl.openal.AL10.AL_OUT_OF_MEMORY
import org.lwjgl.openal.AL10.alBufferData
import org.lwjgl.openal.AL10.alDeleteBuffersDirect
import org.lwjgl.openal.AL10.alDeleteSourcesDirect
import org.lwjgl.openal.AL10.alGenBuffers
import org.lwjgl.openal.AL10.alGenSources
import org.lwjgl.openal.AL10.alGetError
import org.lwjgl.openal.AL10.alSourcei
import org.lwjgl.openal.ALC10.ALC_DEFAULT_DEVICE_SPECIFIER
import org.lwjgl.openal.ALC10.ALC_DEVICE_SPECIFIER
import org.lwjgl.openal.ALC10.ALC_INVALID_CONTEXT
import org.lwjgl.openal.ALC10.ALC_INVALID_DEVICE
import org.lwjgl.openal.ALC10.ALC_INVALID_ENUM
import org.lwjgl.openal.ALC10.ALC_INVALID_VALUE
import org.lwjgl.openal.ALC10.ALC_NO_ERROR
import org.lwjgl.openal.ALC10.ALC_OUT_OF_MEMORY
import org.lwjgl.openal.ALC10.alcGetError
import org.lwjgl.openal.ALC10.alcGetString
import org.lwjgl.openal.ALC10.alcIsExtensionPresent
import org.lwjgl.openal.ALC10.alcMakeContextCurrent
import org.lwjgl.stb.STBVorbis.stb_vorbis_close
import org.lwjgl.stb.STBVorbis.stb_vorbis_get_info
import org.lwjgl.stb.STBVorbis.stb_vorbis_get_samples_short
import org.lwjgl.stb.STBVorbis.stb_vorbis_get_samples_short_interleaved
import org.lwjgl.stb.STBVorbis.stb_vorbis_open_memory
import org.lwjgl.stb.STBVorbis.stb_vorbis_stream_length_in_samples
import org.lwjgl.stb.STBVorbisInfo
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import kotlin.use
import kotlin.Char.Companion.MIN_VALUE as nullChar

private val logger = KotlinLogging.logger {}

private typealias AudioBuffer = Int

object Audio : Disposable {
    fun getAudioDevices(): List<String> {
        if (!alcIsExtensionPresent(0, "ALC_ENUMERATION_EXTENSION")) {
            logger.info { "Audio devices could not be enumerated: returning default" }
            return listOf(alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER)!!)
        }

        val devices = alcGetString(0, ALC_DEVICE_SPECIFIER)!!.split(nullChar)
        logger.info { "Available audio devices: $devices" }
        return devices
    }

    internal val sources = mutableMapOf<AudioContext, MutableList<AudioSource>>()
    internal val buffers = mutableMapOf<AudioContext, MutableList<AudioBuffer>>()
    internal lateinit var currentDevice: AudioDevice
    internal val devices: MutableList<AudioDevice> = mutableListOf()
    internal lateinit var currentContext: AudioContext
    internal val defaultContexts: MutableMap<AudioDevice, AudioContext> = mutableMapOf()

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
        val encodedAudio = ResourceReader.getRawResource(path)
        val pcmAudio = vorbisDecodeToPCM(encodedAudio, mode)

        return createSource(pcmAudio, retainBuffer)
    }

    fun createSource(sampleRate: Int, encodedAudio: ShortBuffer, retainBuffer: Boolean = false): AudioSource {
        checkInit()

        val pcmAudio = PCMAudio(
            sampleRate,
            encodedAudio.capacity(),
            AudioMode.MONO,
            encodedAudio
        ) //TODO update with stereo?

        return createSource(pcmAudio, retainBuffer)
    }

    private fun createSource(pcmAudio: PCMAudio, retainBuffer: Boolean): AudioSource {
        check(pcmAudio.mode != AudioMode.DONT_CARE)

        check(pcmAudio.buffer.isDirect) { "Only direct (off-heap) buffers can be read by OpenAL" }

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
                pcmAudio.buffer.takeIf { retainBuffer }?.let {
                    ShortBuffer.allocate(it.capacity()).put(it).rewind()
                }
            )

            AudioMode.STEREO -> StereoAudioSource(
                source,
                pcmAudio.sampleRate,
                pcmAudio.numSamples,
                pcmAudio.buffer.takeIf { retainBuffer }?.let {
                    ShortBuffer.allocate(it.capacity()).put(it).rewind()
                }
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

private data class PCMAudio(
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

internal fun checkError(message: String = "OpenAL error occurred") {
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

internal fun checkContextError(device: Long, message: String = "OpenALC error occurred") {
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
