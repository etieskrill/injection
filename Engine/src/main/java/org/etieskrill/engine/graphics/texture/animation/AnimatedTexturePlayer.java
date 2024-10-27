package org.etieskrill.engine.graphics.texture.animation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class AnimatedTexturePlayer {

    private final @Getter AnimatedTexture texture;
    private final Behaviour behaviour;

    private @Setter float speed = 1;
    private float time = 0;
    private @Getter boolean playing;

    public enum Behaviour {
        ONCE,
        REPEAT
    }

    public AnimatedTexturePlayer(AnimatedTexture texture) {
        this.texture = texture;
        this.behaviour = Behaviour.REPEAT;
    }

    public void play() {
        time = 0;
        playing = true;
    }

    public void resume() {
        playing = true;
    }

    public void stop() {
        playing = false;
    }

    public void update(double delta) {
        if (playing) {
            time += speed * (float) delta;
        }
    }

    public int getFrame() {
        float time;
        switch (behaviour) {
            case REPEAT -> time = this.time % texture.getMetaData().getDuration();
            case ONCE -> time = this.time;
            case null -> throw new IllegalStateException("Animation behaviour may not be null");
        }

        int numFrames = texture.getMetaData().getFrames().size();
        int index = -1;
        for (int i = 0; i < numFrames - 1; i++) {
            float previousTime = texture.getMetaData().getFrames().get(i).getTime();
            float frameTime = texture.getMetaData().getFrames().get(i + 1).getTime();

            if (frameTime >= time && previousTime <= time) {
                index = i;
                break;
            }
        }

        float lastFrameTime = texture.getMetaData().getFrames().get(numFrames - 1).getTime();
        float animationDuration = texture.getMetaData().getDuration();
        if (index == -1 && time >= lastFrameTime && time < animationDuration) {
            index = numFrames - 1;
        }

        if (index == -1) {
            if (behaviour == Behaviour.REPEAT) {
                index = 0;
            } else if (behaviour == Behaviour.ONCE) {
                index = texture.getMetaData().getFrames().size() - 1;
            }
        }

        return index;
    }

}
