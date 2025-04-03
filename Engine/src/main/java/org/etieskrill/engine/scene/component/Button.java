package org.etieskrill.engine.scene.component;

import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.input.Key;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.input.action.SimpleAction;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static java.util.Objects.requireNonNull;
import static org.etieskrill.engine.scene.component.LayoutUtils.getPreferredNodePosition;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

public class Button extends Node {

    private Label label;
    private SimpleAction action;

    public Button() {
        this(new Label());
    }

    public Button(Label label) {
        setLabel(label);
    }

    @Override
    public void update(double delta) {
        label.update(delta);
    }

    @Override
    public void format() {
        if (!shouldFormat()) return;

        label.format();
        label.setPosition(getPreferredNodePosition(getSize(), label));
    }

    @Override
    public void render(Batch batch) {
        batch.renderBox(
                new Vector3f(getPosition(), 0),
                new Vector3f(getSize(), 0),
                new Vector4f(0, 0, 0, 1)
        );

        label.render(batch);
    }

    public void setLabel(@NotNull Label label) {
        invalidate();
        this.label = (Label) requireNonNull(label).setParent(this);
    }

    public void setAction(@NotNull SimpleAction action) {
        this.action = action;
    }

    @Override
    public boolean hit(Key button, int action, double posX, double posY) {
        if (!doesHit(posX, posY)) return false;
        if (this.action != null && action == GLFW_RELEASE && button.equals(Keys.LEFT_MOUSE)) {
            this.action.run();
            return true;
        }
        return false;
    }

}
