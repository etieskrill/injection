package org.etieskrill.engine.scene.component

import org.joml.Vector2f
import org.joml.Vector4f
import kotlin.time.Duration

class PlaybackBar(
    val totalDuration: Duration,
    var action: (Duration) -> Unit = {}
) : HBox() {

    enum class State { STOPPED, PLAYING, PAUSED }

    var state: State = State.STOPPED
        set(value) {
            field = when (field) {
                State.PLAYING -> when (value) {
                    State.PLAYING -> State.PLAYING
                    State.PAUSED -> {
                        setChild(0, playButton)
                        stopButton.enable()
                        State.PAUSED
                    }

                    State.STOPPED -> {
                        setChild(0, playButton)
                        stopButton.disable()
                        State.STOPPED
                    }
                }

                State.PAUSED -> when (value) {
                    State.PLAYING -> {
                        setChild(0, pauseButton)
                        stopButton.enable()
                        State.PLAYING
                    }

                    State.PAUSED -> State.PAUSED
                    State.STOPPED -> {
                        setChild(0, playButton)
                        stopButton.disable()
                        State.STOPPED
                    }
                }

                State.STOPPED -> when (value) {
                    State.PLAYING -> {
                        setChild(0, pauseButton)
                        stopButton.enable()
                        State.PLAYING
                    }

                    State.PAUSED -> error("Cannot transition from stopped to paused")
                    State.STOPPED -> State.STOPPED
                }
            }
            when (field) {
                State.PLAYING -> playAction()
                State.PAUSED -> pauseAction()
                State.STOPPED -> stopAction()
            }
        }

    var playAction: () -> Unit = {}
    var pauseAction: () -> Unit = {}
    var stopAction: () -> Unit = {}

    private val playButton = Button(Image(/*"textures/"*/"icons/play-button-black.png").setSize(Vector2f(32f))).apply {
        setSize(Vector2f(32f))
        setAction { state = State.PLAYING }
    }
    private val pauseButton =
        Button(Image(/*"textures/"*/"icons/pause-button-black.png").setSize(Vector2f(32f))).apply {
            setSize(Vector2f(32f))
            setAction { state = State.PAUSED }
        }
    private val stopButton = Button(Image(/*"textures/"*/"icons/stop-button-black.png").setSize(Vector2f(32f))).apply {
        setSize(Vector2f(32f))
        setAction { state = State.STOPPED }
    }
    private val progressBar = Slider(0f, 0f, 1f).apply {
        setSize(Vector2f(500f, 15f))
        margin = Vector4f(5f)

        action = {
            time = totalDuration * it.toDouble()
            this@PlaybackBar.action(time)
        }
    }
    private val progressLabel = Label("0:00")

    var time: Duration = Duration.ZERO
        set(value) {
            progressBar.value = time.inWholeMilliseconds.toFloat() / totalDuration.inWholeMilliseconds
            val seconds = value.inWholeSeconds % 60
            progressLabel.text = "${value.inWholeMinutes}:${if (seconds < 10) 0 else ""}${seconds}"
            field = value
        }

    init {
        addChildren(playButton, stopButton, progressBar, progressLabel)
        stopButton.disable()
    }

}