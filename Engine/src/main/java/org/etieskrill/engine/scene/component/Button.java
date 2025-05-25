package org.etieskrill.engine.scene.component;

import org.etieskrill.engine.input.Key;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.input.action.SimpleAction;
import org.jetbrains.annotations.NotNull;

/**
 * A node with a single child, which runs an action when hit.
 * <p>
 * This intercepts the event, i.e. the hit is not propagated to the child.
 */
public class Button extends Container {

    private SimpleAction action;

    private boolean enabled = true;

    public Button() {
        this(new Label("<no content>"));
    }

    public Button(Node<?> child) {
        super(child);
    }

    public void setAction(@NotNull SimpleAction action) { //plz no remove kotlin needs this
        this.action = action;
    }

    @Override
    public boolean handleHit(@NotNull Key button, Keys.@NotNull Action action, double posX, double posY) {
        if (!enabled || !doesHit(posX, posY)) return false;
        if (this.action != null && action == Keys.Action.RELEASE && button.equals(Keys.LEFT_MOUSE.getInput())) {
            this.action.run();
            return true;
        }
        return false;
    }

    public void enable() {
        enabled = true;
        getRenderedColour().set(getColour());
        if (child != null) child.getRenderedColour().set(child.getColour());
    }

    public void disable() {
        enabled = false;
        getRenderedColour().set(getColour()).mul(0.75f);
        if (child != null) child.getRenderedColour().set(child.getColour()).mul(0.75f);
    }

}
