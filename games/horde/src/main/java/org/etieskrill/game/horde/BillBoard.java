package org.etieskrill.game.horde;

import lombok.Data;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public @Data class BillBoard {

    private final Texture2D sprite;
    private final Vector2f size;
    private final Vector3f offset = new Vector3f(0);

    public void setSize(Vector2fc size) {
        this.size.set(size);
    }

    public void setOffset(Vector3fc offset) {
        this.offset.set(offset);
    }

}
