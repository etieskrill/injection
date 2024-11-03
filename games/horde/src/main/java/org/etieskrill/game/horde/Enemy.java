package org.etieskrill.game.horde;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Scripts;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MagFilter;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MinFilter;
import org.etieskrill.engine.graphics.texture.AbstractTexture.Wrapping;
import org.etieskrill.engine.graphics.texture.animation.AnimatedTexture;
import org.etieskrill.engine.graphics.texture.animation.AnimatedTexturePlayer;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

import static org.joml.Math.toRadians;

public class Enemy extends Entity {

    private final Transform transform;
    private final Vector3f deltaPosition;

    private final AnimatedBillBoard billBoard;

    public Enemy(int id, Vector3f playerPosition, Camera camera) {
        super(id);

        transform = addComponent(new Transform().setPosition(new Vector3f(10, 0, 10)));
        deltaPosition = new Vector3f();

        billBoard = addComponent(new AnimatedBillBoard(
                new AnimatedTexturePlayer(
                        AnimatedTexture.builder()
                                .file("zombie.png")
                                .setMipMapping(MinFilter.NEAREST, MagFilter.NEAREST)
                                .setWrapping(Wrapping.CLAMP_TO_EDGE)
                                .build()
                ),
                new Vector2f(.5f)
        ));
        billBoard.getSpritePlayer().play();

        addComponent(new Scripts(List.of(
                delta -> {
                    deltaPosition.set(new Vector3f(playerPosition)
                            .sub(transform.getPosition())
                            .normalize()
                            .mul(0.5f * delta.floatValue()));
                    transform.translate(deltaPosition);
                },
                delta -> billBoard.getSpritePlayer().update(delta),
                delta -> {
                    boolean lookingRight = deltaPosition.rotateY(toRadians(camera.getYaw())).x() >= 0;
                    billBoard.getSize().set(lookingRight ? -.5f : .5f, .5f);
                }
        )));
    }

}
