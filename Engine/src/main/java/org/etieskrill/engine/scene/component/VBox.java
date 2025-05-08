package org.etieskrill.engine.scene.component;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.util.List;

import static org.etieskrill.engine.scene.component.LayoutUtils.getMinNodeSize;
import static org.etieskrill.engine.scene.component.LayoutUtils.getPreferredNodePosition;

public class VBox extends Stack {

    public VBox() {
    }

    public VBox(@NotNull Node<?>... children) {
        super(List.of(children));
    }

    @Override
    public void format() {
        if (!shouldFormat()) return;

        //Pre-calculate the size of the smallest fitting box around the children and position cursors accordingly
        float topPointer = 0, centerPointer = getSize().y() / 2, bottomPointer = getSize().y();
        for (int i = 0; i < getChildren().size(); i++) {
            Node<?> child = getChildren().get(i);
            child.format();

            if (child.getAlignment() == Alignment.FIXED_POSITION) continue;

            float margin = 0;
            Node<?> nextChild;
            if (getChildren().size() - 1 > i && (nextChild = getChildren().get(i + 1)) != null) {
                margin = Math.max(nextChild.getMargin().y(), child.getMargin().x());
            }

            switch (child.getAlignment()) {
                case CENTER, CENTER_LEFT, CENTER_RIGHT -> centerPointer -= child.getSize().y() / 2;
                case BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> bottomPointer -= getMinNodeSize(child).y() - margin;
            }
        }

        //Place children ignoring vertical preference and adjust cursors
        for (int i = 0; i < getChildren().size(); i++) {
            Node<?> child = getChildren().get(i);
            if (child.getAlignment() == Alignment.FIXED_POSITION) continue;
            Vector2f newPos = getPreferredNodePosition(getSize(), child).mul(1, 0);

            child.setPosition(newPos.add(0,
                    switch (child.getAlignment()) {
                        case FIXED_POSITION -> 0;
                        case TOP, TOP_LEFT, TOP_RIGHT -> topPointer;
                        case CENTER, CENTER_LEFT, CENTER_RIGHT -> centerPointer;
                        case BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> bottomPointer;
                    }));

            float margin = 0;
            Node<?> nextChild;
            if (getChildren().size() - 1 > i && (nextChild = getChildren().get(i + 1)) != null) {
                margin = Math.max(nextChild.getMargin().y(), child.getMargin().x());
            }

            float childHeight = child.getSize().y();
            switch (child.getAlignment()) {
                case TOP, TOP_LEFT, TOP_RIGHT -> topPointer += childHeight + margin;
                case CENTER, CENTER_LEFT, CENTER_RIGHT -> centerPointer += childHeight + margin;
                case BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> bottomPointer += childHeight + margin;
            }
        }
    }

}
