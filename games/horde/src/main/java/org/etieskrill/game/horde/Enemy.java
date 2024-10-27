package org.etieskrill.game.horde;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Scripts;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MagFilter;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MinFilter;
import org.etieskrill.engine.graphics.texture.AbstractTexture.Wrapping;
import org.etieskrill.engine.graphics.texture.animation.AnimatedTexture;
import org.etieskrill.engine.graphics.texture.animation.AnimatedTexturePlayer;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

public class Enemy extends Entity {

    private final Transform transform;

    public Enemy(int id, Vector3f playerPosition) {
        super(id);

        transform = addComponent(new Transform().setPosition(new Vector3f(10, 0, 10)));

        var sprite = addComponent(new AnimatedBillBoard(
                new AnimatedTexturePlayer(
                        AnimatedTexture.builder()
                                .file("zombie.png")
                                .setMipMapping(MinFilter.NEAREST, MagFilter.NEAREST)
                                .setWrapping(Wrapping.CLAMP_TO_EDGE)
                                .build()
                ),
                new Vector2f(.5f)
        ));
        sprite.getSpritePlayer().play();

        addComponent(new Scripts(List.of(
//                delta -> transform.translate(new Vector3f(playerPosition)
//                        .sub(transform.getPosition()).normalize().mul(0.5f * delta.floatValue())),
                delta -> sprite.getSpritePlayer().update(delta)
        )));
    }

    private void rotateToHeading(double delta) {
    }

}
