package org.etieskrill.engine.graphics.texture.animation;

import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.joml.Vector2ic;

import java.util.List;

@SuppressWarnings("ClassCanBeRecord")
public class TextureAnimationMetadata {

    private final String textureFile;
    private final Vector2ic frameSize;
    private final AbstractTexture.Format format;
    private final List<TextureAnimationFrame> frames;
    private final float duration;

    public TextureAnimationMetadata(String textureFile, Vector2ic frameSize, AbstractTexture.Format format, List<TextureAnimationFrame> frames, float duration) {
        this.textureFile = textureFile;
        this.frameSize = frameSize;
        this.format = format;
        this.frames = frames;
        this.duration = duration;
    }

    public String getTextureFile() {
        return textureFile;
    }

    public Vector2ic getFrameSize() {
        return frameSize;
    }

    public AbstractTexture.Format getFormat() {
        return format;
    }

    public List<TextureAnimationFrame> getFrames() {
        return frames;
    }

    public float getDuration() {
        return duration;
    }

}
