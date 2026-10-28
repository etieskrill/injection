package org.etieskrill.engine.audio

import net.bifreus.kml.Vec3
import net.bifreus.kml.Vec3c
import org.lwjgl.BufferUtils.createFloatBuffer
import org.lwjgl.openal.AL10.*

class AudioListener {
    var position: Vec3c = Vec3(0f)
        set(value) {
            field = value
            alListener3f(AL_POSITION, value.x, value.y, value.z)
        }

    var direction: Vec3c = Vec3(0f)
        set(value) {
            field = value
            setOrientation(field, up)
        }

    var up: Vec3c = Vec3(0f, 1f, 0f)
        set(value) {
            field = value
            setOrientation(direction, field)
        }

    private val orientationBuffer = createFloatBuffer(6)
    private fun setOrientation(direction: Vec3c, up: Vec3c) {
        orientationBuffer.position(0)
        orientationBuffer.put(direction.x).put(direction.y).put(direction.z)
        orientationBuffer.put(up.x).put(up.y).put(up.z)
        alListenerfv(AL_ORIENTATION, orientationBuffer.rewind())
    }
}
