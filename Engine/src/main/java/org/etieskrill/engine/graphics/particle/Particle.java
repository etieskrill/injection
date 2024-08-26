package org.etieskrill.engine.graphics.particle;

import lombok.Getter;
import lombok.Setter;
import org.joml.*;

import java.util.function.Consumer;

import static lombok.AccessLevel.NONE;
import static lombok.AccessLevel.PACKAGE;

@Getter
public class Particle {

    private final Vector3f position;
    private final @Getter(NONE) Vector3f relativePosition;

    private final Vector3f velocity;
    private @Setter float angularVelocity;

    private final Matrix2f transform;

    private final @Getter(PACKAGE) Vector4f baseColour;
    private final Vector4f colour;

    private @Setter(PACKAGE) float lifetime;

    public Particle() {
        this.position = new Vector3f();
        this.relativePosition = new Vector3f();
        this.velocity = new Vector3f();
        this.transform = new Matrix2f();
        this.baseColour = new Vector4f(1);
        this.colour = new Vector4f(1);
        this.lifetime = 0;
    }

    void update(float delta, ParticleEmitter emitter) {
        lifetime -= delta;
        relativePosition.add(new Vector3f(velocity).mul(delta));
        position.set(relativePosition);
        if (emitter.isParticlesMoveWithEmitter()) {
            position.add(emitter.getTransform().getPosition());
        }
        transform.rotate(angularVelocity * delta);
        emitter.getUpdateFunction().update(lifetime / emitter.getLifetime(), colour.set(baseColour));
    }

    public Vector3fc getPosition() {
        return position;
    }

    void withPosition(Consumer<Vector3f> position) {
        position.accept(relativePosition);
    }

    public Vector4fc getColour() {
        return colour;
    }

}
