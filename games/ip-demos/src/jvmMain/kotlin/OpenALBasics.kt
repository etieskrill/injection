package io.github.etieskrill.games.ip.demos.openal.basics

import org.etieskrill.engine.util.ResourceReader
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.alcCreateContext
import org.lwjgl.openal.ALC11.*
import org.lwjgl.stb.STBVorbis.*
import org.lwjgl.stb.STBVorbisInfo
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer
import kotlin.Char.Companion.MIN_VALUE as nullChar

fun main() {

    //buffer (n, data (in/out)) -> source (n, playback) -> listener (1, receiver)
    //device (hardware) contains buffers
    //context (n) contains sources and single listener

    //AL - core api, focuses on audio manipulation
    //ALC - context api, provides hardware bindings

    check(alcIsExtensionPresent(0, "ALC_ENUMERATION_EXT"))
    val devices = alcGetString(0, ALC_DEVICE_SPECIFIER)!!.split(nullChar)
    println("Available devices: $devices")
    val defaultDevice = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER)
    println("Default device: $defaultDevice")
    println()

    val device = alcOpenDevice(null as ByteBuffer?) //default device
    check(alcGetString(device, ALC_DEVICE_SPECIFIER) == defaultDevice) //device name
    checkContextError(device)

    val deviceCaps = ALC.createCapabilities(device)

    val captureDevices = alcGetString(0, ALC_CAPTURE_DEVICE_SPECIFIER)!!.split(nullChar)
    println("Capture devices: $captureDevices")
    val defaultCaptureDevice = alcGetString(0, ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER)!!
    println("Default capture device: $defaultCaptureDevice")
    println()

    val context = alcCreateContext(device, null as IntBuffer?)
    alcMakeContextCurrent(context)
    checkContextError(device)

    val caps = AL.createCapabilities(deviceCaps)
//    AL.setCurrentProcess(caps)
//    AL.setCurrentThread(caps)
    checkContextError(device)

    val buffer = alGenBuffers()
    checkError("Failed to create buffer")

    val encodedAudio = ResourceReader.getRawResource("audio/pumped-up-kicks-synthwave.ogg")
    val (channels, sampleRate, pcmAudio) = vorbisDecodeToPCM(encodedAudio)

    val format = if (channels == 1) AL_FORMAT_MONO16 else AL_FORMAT_STEREO16
    alBufferData(buffer, format, pcmAudio, sampleRate)
    checkError("Failed to buffer data")

    val source = alGenSources()
    checkError("Failed to create source")

    alSourcei(source, AL_BUFFER, buffer)
    checkError("Failed to bind buffer to source")

    alSourcePlay(source)

    while (true) {
        println("Playing shit")
    }
}

fun vorbisDecodeToPCM(encodedAudio: ByteBuffer): Triple<Int, Int, ShortBuffer> {
    val error = BufferUtils.createIntBuffer(1)
    val decoder = stb_vorbis_open_memory(encodedAudio, error, null)
    if (decoder == 0L) error("Vorbis failed to open audio file. Error: ${error.get()}")

    val channels: Int
    val sampleRate: Int

    STBVorbisInfo.malloc().use { info ->
        stb_vorbis_get_info(decoder, info)
        channels = info.channels()
        sampleRate = info.sample_rate()
    }

    val pcmBuffer = BufferUtils.createShortBuffer(stb_vorbis_stream_length_in_samples(decoder) * channels)

    stb_vorbis_get_samples_short_interleaved(decoder, channels, pcmBuffer)
    stb_vorbis_close(decoder)

    return Triple(channels, sampleRate, pcmBuffer)
}

fun checkError(message: String = "OpenAL error occurred") {
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

fun checkContextError(device: Long, message: String = "OpenALC error occurred") {
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
