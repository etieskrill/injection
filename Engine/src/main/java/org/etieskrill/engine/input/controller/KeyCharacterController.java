package org.etieskrill.engine.input.controller;

import org.etieskrill.engine.input.Input;
import org.etieskrill.engine.input.InputBinding;
import org.etieskrill.engine.input.KeyInputManager;
import org.etieskrill.engine.input.Keys;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.function.Supplier;

/**
 * @param <T> the type of object being controlled
 */
public class KeyCharacterController<T> extends KeyInputManager {

    protected final T target;
    protected float speed = 1;
    protected @Nullable Supplier<Boolean> updateCondition;
    protected final Updatable<T> updateFunction;

    protected final Vector3f deltaPosition;

    public KeyCharacterController(T target, Updatable<T> updateFunction) {
        this.target = target;
        this.updateFunction = updateFunction;

        this.deltaPosition = new Vector3f();

        addBindings(Input.bind(Keys.W).on(InputBinding.Trigger.PRESSED).to(() -> deltaPosition.add(0, 0, 1)));
        addBindings(Input.bind(Keys.S).on(InputBinding.Trigger.PRESSED).to(() -> deltaPosition.add(0, 0, -1)));
        addBindings(Input.bind(Keys.A).on(InputBinding.Trigger.PRESSED).to(() -> deltaPosition.add(1, 0, 0)));
        addBindings(Input.bind(Keys.D).on(InputBinding.Trigger.PRESSED).to(() -> deltaPosition.add(-1, 0, 0)));
        addBindings(Input.bind(Keys.SPACE).on(InputBinding.Trigger.PRESSED).to(() -> deltaPosition.add(0, 1, 0)));
        addBindings(Input.bind(Keys.SHIFT).on(InputBinding.Trigger.PRESSED).to(() -> deltaPosition.add(0, -1, 0)));
    }

    @Override
    public void update(double delta) {
        updateInputManager(delta);
        if (updateCondition == null || updateCondition.get()) {
            updateFunction.update(delta, target, deltaPosition, speed);
        }
        deltaPosition.zero();
    }

    @FunctionalInterface
    public interface Updatable<T> {
        void update(double delta, T target, Vector3f deltaPosition, float speed);
    }

    protected void updateInputManager(double delta) {
        super.update(delta);
    }

    public float getSpeed() {
        return speed;
    }

    public KeyCharacterController<T> setSpeed(float speed) {
        this.speed = speed;
        return this;
    }

    public KeyCharacterController<T> setUpdateCondition(@NotNull Supplier<Boolean> updateCondition) {
        this.updateCondition = updateCondition;
        return this;
    }

}
