package org.etieskrill.game.horde;

public class DashState {

    private final float duration;
    private final float cooldownDuration;
    private final float regularSpeed;
    private final float dashSpeed;

    private boolean active;
    private boolean triggered;
    private float time;
    private float cooldown;

    public DashState(float duration, float cooldownDuration, float regularSpeed, float dashSpeed) {
        this.duration = duration;
        this.cooldownDuration = cooldownDuration;
        this.regularSpeed = regularSpeed;
        this.dashSpeed = dashSpeed;
        this.active = false;
        this.triggered = false;
        this.time = 0;
        this.cooldown = 0;
    }

    public void update(float delta) {
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

}
