package org.etieskrill.engine.scene.component;

import lombok.Getter;
import lombok.Setter;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.text.Font;
import org.etieskrill.engine.graphics.text.Fonts;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.Textures;
import org.etieskrill.engine.input.Key;
import org.etieskrill.engine.input.Keys;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import static java.util.Objects.requireNonNull;
import static org.etieskrill.engine.scene.component.LayoutUtils.getPreferredNodePosition;

/**
 * A collapsible node with a draggable widget bar containing a single child.
 */
public class WidgetContainer extends Node<WidgetContainer> {

    private static final Vector4fc WIDGET_BAR_COLOUR = new Vector4f(128 / 255f, 0, 0, 1); //#800000
    private static final float WIDGET_BAR_HEIGHT = 20;
    private static final Vector4fc WIDGET_CHEVRON_COLOUR = new Vector4f(1);
    private static final float WIDGET_CHEVRON_MARGIN = 2;

    private Node<?> child;

    private @Getter boolean collapsed = false;

    private final Vector2f actualSize = new Vector2f(-1);

    private final Texture2D chevronIcon;

    private final Font titleFont;
    private @Getter @Setter String text;

    public @Getter final float barHeight = WIDGET_BAR_HEIGHT;

    public WidgetContainer(@NotNull Node<?> child) {
        setChild(child);
        this.chevronIcon = Textures.ofFile("textures/icons/chevron-down-solid-black.png");
        this.titleFont = Fonts.getDefault(((int) getBarHeight()) - 4);
    }

    @Override
    public void update(double delta) {
        if (child != null) child.update(delta);
    }

    @Override
    public void format() { //FIXME bar height is still ignored somehow
        if (!shouldFormat()) return;

        if (collapsed) {
            if (actualSize.equals(-1, -1))
                getSize().set(child.getSize().x, getBarHeight());
            else
                getSize().set(actualSize.x, getBarHeight());
        } else {
            if (actualSize.equals(-1, -1))
                getSize().set(child.getSize()).add(0, getBarHeight());
            else
                getSize().set(actualSize.x, actualSize.y + getBarHeight());

            child.setPosition(getPreferredNodePosition(getSize(), child).add(0, getBarHeight()));
            child.format();
        }
    }

    @Override
    public void render(@NotNull Batch batch) {
        var position = getAbsolutePosition();

        batch.renderBox(new Vector3f(position, 0), new Vector3f(getSize().x, getBarHeight(), 0), WIDGET_BAR_COLOUR);
        batch.blit(chevronIcon,
                new Vector2f(position).add(WIDGET_CHEVRON_MARGIN, WIDGET_CHEVRON_MARGIN),
                new Vector2f(getBarHeight() - 2 * WIDGET_CHEVRON_MARGIN),
                collapsed ? (float) Math.toRadians(90) : (float) Math.toRadians(-180),
                WIDGET_CHEVRON_COLOUR
        );
        if (text != null && !text.isBlank()) {
            //TODO use label with autoscaling font instead of hardcoding - scale bar height too actually
            batch.renderText(text, titleFont, new Vector2f(position).add(getBarHeight() + 2, -3));
        }

        if (!collapsed) {
            if (getRenderedColour().w != 0) batch.renderBox(
                    new Vector3f(position, 0),
                    new Vector3f(getSize(), 0),
                    getRenderedColour()
            );
            if (child != null) child.render(batch);
        }
    }

    public Node<?> getChild() {
        return child;
    }

    public void setChild(@NotNull Node<?> child) {
        invalidate();
        requireNonNull(child).setParent(this);
        this.child = child;
    }

    @Override
    public boolean handleHit(@NotNull Key button, Keys.@NotNull Action action, double posX, double posY) {
        if (!doesHit(posX, posY)) return false;

        var position = getAbsolutePosition();
        if (action == Keys.Action.PRESS
            && posX > position.x && posX <= position.x + getBarHeight()
            && posY > position.y && posY <= position.y + getBarHeight()) {
            setCollapsed(!collapsed);
            requestFocus(); //any reason for a resetFocus instead?
            return true;
        }

        return child.handleHit(button, action, posX, posY); //container itself is not hittable
    }

    public void setCollapsed(boolean collapsed) {
        invalidate();
        this.collapsed = collapsed;
    }

    @Override
    public boolean handleDrag(double deltaX, double deltaY, double posX, double posY) {
        if (getAlignment() != Alignment.FIXED_POSITION) return false;

        if (!doesHit(posX, posY)) return false;

        var position = getAbsolutePosition();
        if (!(posX > position.x + getBarHeight() && posX <= position.x + getSize().x
              && posY > position.y && posY <= position.y + getBarHeight()
        )) return false;

        setPosition(getPosition().sub((float) deltaX, (float) deltaY));

        return true;
    }

}
