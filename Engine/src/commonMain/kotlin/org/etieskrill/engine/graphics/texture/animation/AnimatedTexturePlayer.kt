package org.etieskrill.engine.graphics.texture.animation

enum class AnimatedTexturePlayerBehaviour { ONCE, REPEAT }

class AnimatedTexturePlayer(
    val texture: AnimatedTexture,
    private val behaviour: AnimatedTexturePlayerBehaviour = AnimatedTexturePlayerBehaviour.REPEAT
) {

    var speed = 1f
    private var time = 0f
    var playing = false

    fun play() {
        time = 0f
        playing = true
    }

    fun resume() {
        playing = true
    }

    fun stop() {
        playing = false
    }

    fun update(delta: Double) {
        if (playing) {
            time += speed * delta.toFloat()
        }
    }

    fun getFrame(): Int {
        val time = when (behaviour) {
            AnimatedTexturePlayerBehaviour.REPEAT -> time % texture.metadata.duration
            AnimatedTexturePlayerBehaviour.ONCE -> time
        }

        val frames = texture.metadata.frames

        return when {
            time <= frames[0].time -> 0
            time >= frames.last().time -> frames.lastIndex
            else -> frames.indices.drop(1)
                .indexOfFirst { i -> time in frames[i - 1].time..frames[i].time }
        }
    }

}
