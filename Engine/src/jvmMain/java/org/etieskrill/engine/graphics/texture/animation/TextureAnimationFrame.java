package org.etieskrill.engine.graphics.texture.animation;

import org.joml.primitives.Rectanglei;

@SuppressWarnings("ClassCanBeRecord")
public class TextureAnimationFrame {

    private final Rectanglei atlasArea;
    private final float time;

    public TextureAnimationFrame(Rectanglei atlasArea, float time) {
        this.atlasArea = atlasArea;
        this.time = time;
    }

    public Rectanglei getAtlasArea() {
        return atlasArea;
    }

    public float getTime() {
        return time;
    }

}
