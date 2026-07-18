package org.etieskrill.engine.graphics.texture.animation;

import org.etieskrill.engine.window.Window;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AnimatedTexturePlayerTest {

    AnimatedTexturePlayer fixture;
    AnimatedTexture texture;

    @BeforeEach
    void setUp() {
        Window.builder().build();
        texture = AnimatedTexture.builder().file("zombie.png").build();
        fixture = new AnimatedTexturePlayer(texture);
    }

    @Test
    void shouldNotPlayByDefault() {
        assertFalse(fixture.isPlaying());
    }

    @Test
    void update() {
        fixture.play();

        fixture.update(.1);
        assertEquals(0, fixture.getFrame());

        fixture.update(.1);
        assertEquals(1, fixture.getFrame());

        fixture.update(.1);
        assertEquals(2, fixture.getFrame());
    }

    @Test
    void shouldNotUpdateWhenNotPlaying() {
        fixture.update(.2);
        assertEquals(0, fixture.getFrame());

        fixture.update(.2);
        assertEquals(0, fixture.getFrame());

        assertFalse(fixture.isPlaying());
    }

    @Test
    void shouldLoopOnRepeat() {
        fixture.play();

        float duration = fixture.getTexture().getMetaData().getDuration() / 1000;

        assertEquals(0, fixture.getFrame());

        fixture.update(.5f * duration);
        assertEquals(4, fixture.getFrame());

        fixture.update(.5f * duration);
        assertEquals(0, fixture.getFrame());

        fixture.update(.5f * duration);
        assertEquals(4, fixture.getFrame());

        fixture.update(.5f * duration);
        assertEquals(0, fixture.getFrame());
    }

    @Test
    void shouldNotLoopOnOnce() {
        fixture = new AnimatedTexturePlayer(texture, AnimatedTexturePlayer.Behaviour.ONCE);

        fixture.play();

        float duration = fixture.getTexture().getMetaData().getDuration() / 1000;

        assertEquals(0, fixture.getFrame());

        fixture.update(.5f * duration);
        assertEquals(4, fixture.getFrame());

        fixture.update(.5f * duration);
        assertEquals(8, fixture.getFrame());

        fixture.update(.5f * duration);
        assertEquals(8, fixture.getFrame());
    }

    @Test
    void shouldPlayAllFrames() {
        fixture.play();

        fixture.update(.975);
        assertEquals(7, fixture.getFrame());

        fixture.update(.1);
        assertEquals(8, fixture.getFrame());

        fixture.update(.1);
        assertEquals(0, fixture.getFrame());
    }

    @Test
    void setSpeed() {
        fixture.setSpeed(2);
        fixture.play();

        fixture.update(.05);
        assertEquals(0, fixture.getFrame());

        fixture.update(.05);
        assertEquals(1, fixture.getFrame());

        fixture.update(.05);
        assertEquals(2, fixture.getFrame());
    }

}
