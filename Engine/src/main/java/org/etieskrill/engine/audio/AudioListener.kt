package org.etieskrill.engine.audio

import org.joml.Vector3f
import org.joml.Vector3fc
import org.lwjgl.BufferUtils.createFloatBuffer
import org.lwjgl.openal.AL10.AL_ORIENTATION
import org.lwjgl.openal.AL10.AL_POSITION
import org.lwjgl.openal.AL10.alListener3f
import org.lwjgl.openal.AL10.alListenerfv

class AudioListener {
    var position: Vector3fc = Vector3f(0f)
        set(value) {
            field = value
            alListener3f(AL_POSITION, value.x(), value.y(), value.z())
        }

    var direction: Vector3fc = Vector3f(0f)
        set(value) {
            field = value
            setOrientation(field, up)
        }

    var up: Vector3fc = Vector3f(0f, 1f, 0f)
        set(value) {
            field = value
            setOrientation(direction, field)
        }

    private val orientationBuffer = createFloatBuffer(6)
    private fun setOrientation(direction: Vector3fc, up: Vector3fc) {
        orientationBuffer.position(0)
        direction.get(orientationBuffer).position(3)
        up.get(orientationBuffer)
        alListenerfv(AL_ORIENTATION, orientationBuffer.rewind())
    }
}
