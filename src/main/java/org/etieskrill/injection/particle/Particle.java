package org.etieskrill.injection.particle;

import org.etieskrill.injection.math.Vector2;

public class Particle {

    private final float radius;
    private final Vector2 pos;
    private final Vector2 posPrev;
    private final Vector2 acc;

    private float temp;

    public Particle(float radius, Vector2 pos) {
        this(radius, pos, pos);
    }

    public Particle(float radius, Vector2 posPrev, Vector2 pos) {
        this.radius = radius;
        this.pos = new Vector2(pos);
        this.posPrev = new Vector2(posPrev);
        this.acc = new Vector2();
    }

    public void update(float delta) {
        /*this.vel.set(vel.add(acc.scl(delta)));
        this.pos.set(pos.add(vel.scl(delta)));
        acc.setZero();*/

        Vector2 vel = pos.sub(posPrev);
        posPrev.set(pos);
        pos.set(pos.add(vel).add(acc.scl(delta * delta)));
        acc.setZero();
        temp *= Math.max(1f - (0.3f * delta), 0f);
    }

    public void accelerate(Vector2 vec) {
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

    public Vector2 getPos() {
        return pos;
    }

    public void setPos(Vector2 vec) {
        this.pos.set(vec);
    }

    /*public void setVelZero() {
        this.vel.setZero();
    }

    public Vector2 getVel() {
        return vel;
    }*/
    
    public Vector2 getAcc() {
        return acc;
    }
    
}
