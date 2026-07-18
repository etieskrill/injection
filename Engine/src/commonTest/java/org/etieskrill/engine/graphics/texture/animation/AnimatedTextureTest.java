package org.etieskrill.engine.graphics.texture.animation;

import org.etieskrill.engine.window.Window;
import org.joml.Vector2i;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimatedTextureTest {

    static final String ANIMATED_TEXTURE_FILE = "zombie.png";

    AnimatedTexture fixture;

    @BeforeAll
    static void setUpBeforeClass() {
        Window.builder().build(); //TODO scuffed workaround for context initialisation, do more modularly, or separate cpu and gpu side stuff more strictly
    }

    @BeforeEach
    void setUp() {
        fixture = AnimatedTexture.builder().file(ANIMATED_TEXTURE_FILE).build();
    }

    @Test
    void getTexture() {
        assertEquals(new Vector2i(32), fixture.getTexture().getPixelSize());
        assertEquals(9, fixture.getTexture().getLength());

//        var texture = fixture.getTexture();
//        ByteBuffer pixels = createByteBuffer(
//                fixture.getMetaData().getFrameSize().x()
//                        * fixture.getMetaData().getFrameSize().y()
//                        * fixture.getMetaData().getFrames().size()
//                        * fixture.getTexture().getFormat().getChannels());
//
//        for (int i = 0; i < 9; i++) {
//            GLUtils.checkError();
//            glGetTextureSubImage(
//                    texture.getID(), 0,
//                    0, 0, i,
//                    32, 32, 1,
//                    texture.getFormat().toGLFormat(), GL_UNSIGNED_BYTE, pixels);
//            GLUtils.checkErrorThrowing();
//
//            assertEquals();
//        }
    }

}