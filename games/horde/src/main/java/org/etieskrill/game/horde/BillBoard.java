package org.etieskrill.game.horde;

import lombok.Getter;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.UniformMappable;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Getter
public class BillBoard implements UniformMappable {

    private final Texture2D sprite;
    private final Vector2f size;
    private final Vector3f offset = new Vector3f(0);
    private final boolean punchThrough;

    public BillBoard(Texture2D sprite, Vector2f size) {
        this(sprite, size, false);
    }

    public BillBoard(Texture2D sprite, Vector2f size, boolean punchThrough) {
        this.sprite = sprite;
        this.size = size;
        this.punchThrough = punchThrough;
    }

    public void setSize(Vector2fc size) {
        this.size.set(size);
    }

    public void setOffset(Vector3fc offset) {
        this.offset.set(offset);
    }

    @Override
    public boolean map(ShaderProgram.UniformMapper mapper) {
        mapper
                .map("sprite", sprite)
                .map("size", size)
                .map("offset", offset)
                .map("punchThrough", punchThrough);
        return true;
    }

}
