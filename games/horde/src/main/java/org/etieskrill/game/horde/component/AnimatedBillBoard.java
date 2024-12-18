package org.etieskrill.game.horde.component;

import lombok.Getter;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.UniformMappable;
import org.etieskrill.engine.graphics.texture.animation.AnimatedTexturePlayer;
import org.joml.Vector2f;
import org.joml.Vector3f;

@Getter
public class AnimatedBillBoard implements UniformMappable {

    private final AnimatedTexturePlayer spritePlayer;
    private final Vector2f size;
    private final Vector3f offset = new Vector3f(0);
    private final boolean punchThrough;

    public AnimatedBillBoard(AnimatedTexturePlayer sprite, Vector2f size) {
        this(sprite, size, false);
    }

    public AnimatedBillBoard(AnimatedTexturePlayer sprite, Vector2f size, boolean punchThrough) {
        this.spritePlayer = sprite;
        this.size = size;
        this.punchThrough = punchThrough;
    }

    @Override
    public boolean map(ShaderProgram.UniformMapper mapper) {
        mapper
                .map("sprite", spritePlayer.getTexture().getTexture())
                .map("layer", spritePlayer.getFrame())
                .map("size", size)
                .map("offset", offset)
                .map("punchThrough", punchThrough);
        return true;
    }

}
