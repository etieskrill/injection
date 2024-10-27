package org.etieskrill.engine.graphics.texture.animation;

import lombok.Data;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.joml.Vector2ic;

import java.util.List;

@Data
public class TextureAnimationMetadata {
    private final String textureFile;
    private final Vector2ic frameSize;
    private final AbstractTexture.Format format;
    private final List<TextureAnimationFrame> frames;
    private final float duration;
}
