package org.etieskrill.engine.graphics.texture.animation;

import org.joml.Vector2i;
import org.joml.primitives.Rectanglei;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextureAnimationYamlParserTest {

    private static final String TEXTURE_META_FILE = "zombie.tex-anim.yml";
    private static final String TEXTURE_FILE = "zombie.png";

    @Test
    void loadAnimationMetadata() {
        var metaData = TextureAnimationYamlParser.loadAnimationMetadata(TEXTURE_META_FILE);

        assertEquals(TEXTURE_FILE, metaData.getTextureFile());
        assertEquals(new Vector2i(32), metaData.getFrameSize());
        assertEquals(List.of(
                        new TextureAnimationFrame(new Rectanglei(0, 0, 32, 32), 0),
                        new TextureAnimationFrame(new Rectanglei(32, 0, 64, 32), 125),
                        new TextureAnimationFrame(new Rectanglei(64, 0, 96, 32), 250),
                        new TextureAnimationFrame(new Rectanglei(96, 0, 128, 32), 375),
                        new TextureAnimationFrame(new Rectanglei(128, 0, 160, 32), 475),
                        new TextureAnimationFrame(new Rectanglei(160, 0, 192, 32), 600),
                        new TextureAnimationFrame(new Rectanglei(192, 0, 224, 32), 750),
                        new TextureAnimationFrame(new Rectanglei(224, 0, 256, 32), 875),
                        new TextureAnimationFrame(new Rectanglei(256, 0, 288, 32), 1000)),
                metaData.getFrames());
    }

}
