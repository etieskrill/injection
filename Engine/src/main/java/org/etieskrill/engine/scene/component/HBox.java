package org.etieskrill.engine.scene.component;

import glm_.vec2.Vec2;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.etieskrill.engine.scene.component.LayoutUtils.getMinNodeSize;
import static org.etieskrill.engine.scene.component.LayoutUtils.getPreferredNodePosition;

public class HBox extends Stack {

    public HBox() {
    }

    public HBox(@NotNull Node... children) {
        super(List.of(children));
    }

    @Override
    public void format() {
        if (!shouldFormat()) return;

        //Pre-calculate the size of the smallest fitting box around the children and position cursors accordingly
        float topPointer = 0, centerPointer = getSize().getX() / 2, bottomPointer = getSize().getX();
        for (int i = 0; i < getChildren().size(); i++) {
            Node child = getChildren().get(i);
            child.format();

            float margin = 0;
            Node nextChild;
            if (getChildren().size() - 1 > i && (nextChild = getChildren().get(i + 1)) != null) {
                margin = Math.max(nextChild.getMargin().getZ(), child.getMargin().getW());
            }

            switch (child.getAlignment()) {
                case CENTER, CENTER_LEFT, CENTER_RIGHT -> centerPointer -= child.getSize().getX() / 2;
                case BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> bottomPointer -= getMinNodeSize(child).getX() - margin;
            }
        }

        //Place children ignoring vertical preference and adjust cursors
        for (int i = 0; i < getChildren().size(); i++) {
            Node child = getChildren().get(i);
            Vec2 newPos = getPreferredNodePosition(getSize(), child).times(0, 1);

            child.setPosition(newPos.plus(
                    (float) switch (child.getAlignment()) {
                        case TOP, TOP_LEFT, TOP_RIGHT -> topPointer;
                        case CENTER, CENTER_LEFT, CENTER_RIGHT -> centerPointer;
                        case BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> bottomPointer;
                    }, 0));

            float margin = 0;
            Node nextChild;
            if (getChildren().size() - 1 > i && (nextChild = getChildren().get(i + 1)) != null) {
                margin = Math.max(nextChild.getMargin().getZ(), child.getMargin().getW());
            }

            float childWidth = child.getSize().getX();
            switch (child.getAlignment()) {
                case TOP, TOP_LEFT, TOP_RIGHT -> topPointer += childWidth + margin;
                case CENTER, CENTER_LEFT, CENTER_RIGHT -> centerPointer += childWidth + margin;
                case BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> bottomPointer += childWidth + margin;
            }
        }
    }

}
