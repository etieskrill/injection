package org.etieskrill.engine.graphics.texture.animation;

import org.etieskrill.engine.graphics.texture.Texture;
import org.joml.Vector2ic;

import java.util.List;

@SuppressWarnings("ClassCanBeRecord")
public class TextureAnimationMetadata {

    private final String textureFile;
    private final Vector2ic frameSize;
    private final Texture.Format format;
    private final List<TextureAnimationFrame> frames;
    private final float duration;

    public TextureAnimationMetadata(String textureFile, Vector2ic frameSize, Texture.Format format, List<TextureAnimationFrame> frames, float duration) {
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

    public Texture.Format getFormat() {
        return format;
    }

    public List<TextureAnimationFrame> getFrames() {
        return frames;
    }

    public float getDuration() {
        return duration;
    }

}
