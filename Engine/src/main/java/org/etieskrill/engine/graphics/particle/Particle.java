package org.etieskrill.engine.graphics.particle;

import org.joml.Vector3f;
import org.joml.Vector4f;

public class Particle {

    private final Vector3f position;
    private final Vector3f velocity;
    private final Vector4f colour;

    private float lifetime;

    public Particle() {
        this.position = new Vector3f();
        this.velocity = new Vector3f();
        this.colour = new Vector4f(1);
        this.lifetime = 0;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getVelocity() {
        return velocity;
    }

    public Vector4f getColour() {
        return colour;
    }

    public float getLifetime() {
        return lifetime;
    }

    public void setLifetime(float lifetime) {
        this.lifetime = lifetime;
    }

    public void update(float delta) {
        position.add(new Vector3f(velocity).mul(delta));
        lifetime -= delta;
    }

}
