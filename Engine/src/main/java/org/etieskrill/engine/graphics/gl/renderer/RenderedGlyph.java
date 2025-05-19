package org.etieskrill.engine.graphics.gl.renderer;

import lombok.Data;
import org.joml.Vector2f;

@Data
public class RenderedGlyph {
    private final Vector2f size = new Vector2f();
    private final Vector2f position = new Vector2f();
    private int textureIndex;
}
