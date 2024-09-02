package org.etieskrill.game.horde3d;

import static java.lang.Math.clamp;

public class DashState {

    private final float duration;
    private final float cooldownDuration;
    private final float regularSpeed;
    private final float dashSpeed;

    private boolean active;
    private boolean triggered;
    private float time;
    private float cooldown;

    private float activeFactor;

    public DashState(float duration, float cooldownDuration, float regularSpeed, float dashSpeed) {
        this.duration = duration;
        this.cooldownDuration = cooldownDuration;
        this.regularSpeed = regularSpeed;
        this.dashSpeed = dashSpeed;
        this.active = false;
        this.triggered = false;
        this.time = 0;
        this.cooldown = 0;
        this.activeFactor = 0;
    }

    public void update(float delta) {
        activeFactor -= (activeFactor - (active ? 1 : 0)) * delta * 15;
        activeFactor = clamp(activeFactor, 0, 1);

        if (triggered) {
            if (!active && cooldown <= 0) {
                active = true;
            }
            triggered = false;
        }

        if (!active) {
            if (cooldown > 0) cooldown -= delta;
            return;
        }

        time += delta;
        if (time > duration) {
            time = 0;
            active = false;
            cooldown = cooldownDuration;
        }
    }

    public float getRegularSpeed() {
        return regularSpeed;
    }

    public float getDashSpeed() {
        return dashSpeed;
    }

    public boolean isActive() {
        return active;
    }

    public void trigger() {
        triggered = true;
    }

    public float getTime() {
        return time;
    }

    public float getCooldown() {
        return cooldown;
    }

    public float getActiveFactor() {
        return activeFactor;
    }

}
