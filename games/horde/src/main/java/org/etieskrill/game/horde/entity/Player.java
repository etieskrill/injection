package org.etieskrill.game.horde.entity;

import lombok.Getter;
import lombok.Setter;
import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Scripts;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MagFilter;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MinFilter;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.time.LoopPacer;
import org.etieskrill.game.horde.component.BillBoard;
import org.etieskrill.game.horde.component.Collider;
import org.etieskrill.game.horde.component.EffectContainer;
import org.etieskrill.game.horde.component.MovementSpeed;
import org.joml.Vector2f;

import java.util.List;

import static org.joml.Math.sin;

@Getter
@Setter
public class Player extends Entity {

    private final Transform transform;
    private final BillBoard billBoard;
    private final MovementSpeed movementSpeed;

    private boolean walking = false;
    private boolean lookingRight = true;

    //                    TODO bundle in ... SceneContext? window inputs should be bound to scenes anyway
    public Player(int id, LoopPacer pacer) {
        super(id);

        transform = addComponent(new Transform());
        billBoard = addComponent(new BillBoard(
                new Texture2D.FileBuilder("dude.png")
                        .setMipMapping(MinFilter.NEAREST, MagFilter.NEAREST).build(),
                new Vector2f()
        ));
        addComponent(new Collider(.2f));

        addComponent(new Scripts(List.of(
                delta -> billBoard.getSize().set(lookingRight ? -.5f : .5f, .5f),
                delta -> {
                    float horizontalOffset = 0, verticalOffset = 0, rotation = 0;
                    if (walking) {
                        verticalOffset = (float) (0.075f * sin(20 * pacer.getTime()) + 0.075f);
                        horizontalOffset = (float) (0.05f * sin(10 * pacer.getTime()));
                        rotation = (float) (0.1f * sin(10 * pacer.getTime()));
                    }

                    billBoard.getOffset().set(horizontalOffset, verticalOffset, 0);
                    billBoard.setRotation(rotation);
                }
        )));

        movementSpeed = addComponent(new MovementSpeed());

        addComponent(new EffectContainer());
    }

}
