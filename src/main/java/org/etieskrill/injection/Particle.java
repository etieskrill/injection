package org.etieskrill.injection;

public class Particle {

    private final float radius;
    private final Vector2 pos;
    private final Vector2 posPrev;
    private final Vector2 acc;

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
    }

    public void accelerate(Vector2 vec) {
        this.acc.set(this.acc.add(vec));
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

}