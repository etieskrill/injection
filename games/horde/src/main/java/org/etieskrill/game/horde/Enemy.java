package org.etieskrill.game.horde;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Scripts;
import org.etieskrill.engine.entity.component.Transform;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

import static org.etieskrill.game.horde.Application.getPixelTexture;

public class Enemy extends Entity {

    private final Transform transform;

    public Enemy(int id, Vector3f playerPosition) {
        super(id);

        transform = addComponent(new Transform().setPosition(new Vector3f(10, 0, 10)));
        addComponent(new BillBoard(
                getPixelTexture("dude.png"),
                new Vector2f(0.5f)
        ));
        addComponent(new Scripts(List.of(
                delta -> transform.translate(new Vector3f(playerPosition)
                        .sub(transform.getPosition()).normalize().mul(0.5f * delta.floatValue()))
        )));
    }

    private void rotateToHeading(double delta) {
    }

}
