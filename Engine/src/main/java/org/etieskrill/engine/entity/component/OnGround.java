package org.etieskrill.engine.entity.component;

public class OnGround {

    private final float jumpStrength;
    private final float bumpStrength;

    private boolean onGround = true;

    public OnGround(float jumpStrength, float bumpStrength) {
        this.jumpStrength = jumpStrength;
        this.bumpStrength = bumpStrength;
    }

    public float getJumpStrength() {
        return jumpStrength;
    }

    public float getBumpStrength() {
        return bumpStrength;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

}
