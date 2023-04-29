package org.etieskrill.injection.particle;

import org.etieskrill.injection.math.Vector2f;

public class Particle {

    private final float radius;
    private final Vector2f pos;
    private final Vector2f posPrev;
    private final Vector2f acc;

    private float temp;

    public Particle(float radius, Vector2f pos) {
        this(radius, pos, pos);
    }

    public Particle(float radius, Vector2f posPrev, Vector2f pos) {
        this.radius = radius;
        this.pos = new Vector2f(pos);
        this.posPrev = new Vector2f(posPrev);
        this.acc = new Vector2f();
    }

    public void update(float delta) {
        /*this.vel.set(vel.add(acc.scl(delta)));
        this.pos.set(pos.add(vel.scl(delta)));
        acc.setZero();*/

        Vector2f vel = pos.sub(posPrev);
        posPrev.set(pos);
        pos.set(pos.add(vel).add(acc.scl(delta * delta)));
        acc.setZero();
        temp *= Math.max(1f - (0.3f * delta), 0f);
    }

    public void accelerate(Vector2f vec) {
        this.acc.set(this.acc.add(vec));
    }
    
    public void heat(float h) {
        temp = Math.max(Math.min(1f, temp + h), 0f);
    }
    
    public float getTemp() {
        return temp;
    }
    
    public float getRadius() {
        return radius;
    }

    public Vector2f getPos() {
        return pos;
    }
    
    public Vector2f getPosPrev() {
        return posPrev;
    }

    public void setPos(Vector2f vec) {
        this.pos.set(vec);
    }
    
    public void setPosPrev(Vector2f vec) {
        this.posPrev.set(vec);
    }
    
    /*public void setVelZero() {
        this.vel.setZero();
    }

    public Vector2f getVel() {
        return vel;
    }*/
    
    public Vector2f getAcc() {
        return acc;
    }
    
}
