package org.etieskrill.engine.graphics.texture.animation;

import lombok.Data;
import org.joml.primitives.Rectanglei;

@Data
public class TextureAnimationFrame {
    private final Rectanglei atlasArea;
    private final float time;
}
