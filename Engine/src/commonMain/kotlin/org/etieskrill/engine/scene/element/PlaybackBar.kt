package org.etieskrill.engine.scene.element

import net.bifreus.kml.Vec2
import net.bifreus.kml.Vec4
import org.etieskrill.engine.graphics.text.Font
import org.etieskrill.engine.scene.container.HBox
import kotlin.time.Duration

class PlaybackBar(
    var totalDuration: Duration,
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

    private val playButton = Button(
        Image("textures/icons/play-button-black.png").apply { size = Vec2(32f) }
    ) { state = State.PLAYING }.apply { size = Vec2(32f) }
    private val pauseButton = Button(
        Image("textures/icons/pause-button-black.png").apply { size = Vec2(32f) }
    ) { state = State.PAUSED }.apply { size = Vec2(32f) }
    private val stopButton = Button(
        Image("textures/icons/stop-button-black.png").apply { size = Vec2(32f) }
    ) { state = State.STOPPED }.apply { size = Vec2(32f) }
    private val progressBar = Slider(0f, 0f, 1f).apply {
        size = Vec2(500f, 15f)
        margin = Vec4(5f)
        colour = Vec4(0f, 0f, 0f, 1f)

        this.action = {
            time = totalDuration * it.toDouble()
            this@PlaybackBar.action(time)
        }
    }
    private val progressLabel = Label("0:00").apply {
        scaleMode = ScaleMode.FIXED
        size = Vec2(50f, Font.DEFAULT_FONT_SIZE.toFloat())
    }

    var time: Duration = Duration.ZERO
        set(value) {
            progressBar.value = value.inWholeMilliseconds.toFloat() / totalDuration.inWholeMilliseconds
            val seconds = value.inWholeSeconds % 60
            progressLabel.text = "${value.inWholeMinutes}:${if (seconds < 10) 0 else ""}${seconds}"
            field = value
        }

    init {
        addChildren(playButton, stopButton, progressBar, progressLabel)
        stopButton.disable()
    }

}
