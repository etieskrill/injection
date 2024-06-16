package org.etieskrill.engine.entity.component;

public class OnGround {

    private final float jumpStrength;

    private boolean onGround = true;

    public OnGround(float jumpStrength) {
        this.jumpStrength = jumpStrength;
    }

    public float getJumpStrength() {
        return jumpStrength;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

}
