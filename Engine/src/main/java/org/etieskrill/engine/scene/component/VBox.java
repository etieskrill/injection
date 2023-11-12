package org.etieskrill.engine.scene.component;

import glm_.vec2.Vec2;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.etieskrill.engine.scene.component.LayoutUtils.getPreferredNodePosition;

public class VBox extends Stack {

    public VBox() {
    }

    public VBox(@NotNull Node... children) {
        super(List.of(children));
    }

    @Override
    public void format() {
        if (!shouldFormat() || getChildren().stream().noneMatch(child -> child.shouldFormat)) return;

        float topPointer = 0, centerPointer = 0, bottomPointer = getSize().getY();
        for (Node child : getChildren()) {
            child.format();
            switch (child.getAlignment()) {
                case CENTER, CENTER_LEFT, CENTER_RIGHT -> centerPointer -= child.getSize().getY() / 2;
            }
        }

        for (int i = 0; i < getChildren().size(); i++) {
            Node child = getChildren().get(i);
            Vec2 newPos = getPreferredNodePosition(getSize(), child);

            child.setPosition(newPos.plus(0,
                    (float) switch (child.getAlignment()) {
                        case TOP, TOP_LEFT, TOP_RIGHT -> topPointer;
                        case CENTER, CENTER_LEFT, CENTER_RIGHT -> centerPointer;
                        case BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> bottomPointer;
                    }));

            float margin = 0;
            Node nextChild;
            if (getChildren().size() - 1 > i && (nextChild = getChildren().get(i + 1)) != null) {
                margin = Math.max(nextChild.getMargin().getY(), child.getMargin().getX());
            }

            float childHeight = child.getSize().getY();
            switch (child.getAlignment()) {
                case TOP, TOP_LEFT, TOP_RIGHT -> topPointer += childHeight + margin;
                case CENTER, CENTER_LEFT, CENTER_RIGHT -> centerPointer += childHeight + margin;
                case BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> bottomPointer -= childHeight + margin;
            }
        }
    }

}
