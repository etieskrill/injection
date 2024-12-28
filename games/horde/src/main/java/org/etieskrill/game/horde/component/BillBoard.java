package org.etieskrill.game.horde.component;

import lombok.Getter;
import lombok.Setter;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.UniformMappable;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.joml.Vector2f;
import org.joml.Vector3f;

@Getter
public class BillBoard implements UniformMappable {

    private final Texture2D sprite;
    private final Vector2f size;
    private final Vector3f offset = new Vector3f(0);
    private @Setter float rotation = 0;
    private final boolean punchThrough;

    public BillBoard(Texture2D sprite, Vector2f size) {
        this(sprite, size, false);
    }

    public BillBoard(Texture2D sprite, Vector2f size, boolean punchThrough) {
        this.sprite = sprite;
        this.size = size;
        this.punchThrough = punchThrough;
    }

    @Override
    public boolean map(ShaderProgram.UniformMapper mapper) {
        mapper
                .map("sprite", sprite)
                .map("size", size)
                .map("offset", offset)
                .map("rotation", rotation)
                .map("punchThrough", punchThrough);
        return true;
    }

}
