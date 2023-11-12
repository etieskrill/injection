package org.etieskrill.engine.scene.component;

import glm_.vec2.Vec2;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.etieskrill.engine.scene.component.LayoutUtils.getPreferredNodePosition;

public class HBox extends Stack {

    public HBox() {
    }

    public HBox(@NotNull Node... children) {
        super(List.of(children));
    }

    @Override
    public void format() {
        if (!shouldFormat() || getChildren().stream().noneMatch(child -> child.shouldFormat)) return;

        float leftPointer = 0, centerPointer = 0, rightPointer = getSize().getX();
        for (Node child : getChildren()) {
            child.format();
            switch (child.getAlignment()) {
                case CENTER, TOP, BOTTOM -> centerPointer -= child.getSize().getX() / 2; //TODO fix
            }
        }

        for (int i = 0; i < getChildren().size(); i++) {
            Node child = getChildren().get(i);
            Vec2 newPos = getPreferredNodePosition(getSize(), child);

            child.setPosition(newPos.plus(
                    (float) switch (child.getAlignment()) {
                        case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> leftPointer;
                        case TOP, CENTER, BOTTOM -> centerPointer;
                        case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> rightPointer;
                    }, 0));

            float margin = 0;
            Node nextChild;
            if (getChildren().size() - 1 > i && (nextChild = getChildren().get(i + 1)) != null) {
                margin = Math.max(nextChild.getMargin().getW(), child.getMargin().getZ());
            }

            float childWidth = child.getSize().getX();
            switch (child.getAlignment()) {
                case TOP, TOP_LEFT, TOP_RIGHT -> leftPointer += childWidth + margin;
                case CENTER, CENTER_LEFT, CENTER_RIGHT -> centerPointer += childWidth + margin;
                case BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> rightPointer -= childWidth + margin;
            }
        }
    }

}
