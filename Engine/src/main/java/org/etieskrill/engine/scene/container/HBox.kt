package org.etieskrill.engine.scene.container;

import org.etieskrill.engine.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.util.List;

import static org.etieskrill.engine.scene.LayoutUtilsKt.getMinNodeSize;
import static org.etieskrill.engine.scene.LayoutUtilsKt.getPreferredNodePosition;

public class HBox extends Stack {

    public HBox() {
    }

    public HBox(@NotNull Node<?>... children) {
        super(List.of(children));
    }

    @Override
    protected void computeBoundingBox() {
        float width = 0, height = 0;

        for (Node<?> child : getChildren()) {
            var nodeSize = getMinNodeSize(child);
            width += nodeSize.x;
            height = Math.max(height, nodeSize.y);
        }

        getFormattedSize().set(width, height);
    }

    @Override
    public void layout() {
        if (!shouldFormat()) return;

        //Pre-calculate the size of the smallest fitting box around the children and position cursors accordingly
        float leftPointer = 0, centerPointer = getFormattedSize().x() / 2, rightPointer = getFormattedSize().x();
        float numLeftGrow = 0, numCenterGrow = 0, numRightGrow = 0;
        for (int i = 0; i < getChildren().size(); i++) {
            Node<?> child = getChildren().get(i);

            if (child.getScaleMode() == ScaleMode.GROW) {
                switch (child.getAlignment()) {
                    case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> numLeftGrow++;
                    case TOP, CENTER, BOTTOM -> numCenterGrow++;
                    case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> numRightGrow++;
                }
                continue;
            }

            child.computeFixedSizes();

            if (child.getAlignment() == Node.Alignment.FIXED_POSITION) continue;

            float margin = 0;
            Node<?> nextChild;
            if (getChildren().size() - 1 > i && (nextChild = getChildren().get(i + 1)) != null) {
                margin = Math.max(nextChild.getMargin().z(), child.getMargin().w());
            }

            switch (child.getAlignment()) {
                case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> leftPointer += getMinNodeSize(child).x - margin;
                case TOP, CENTER, BOTTOM -> centerPointer -= child.getFormattedSize().x() / 2f;
                case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> rightPointer -= getMinNodeSize(child).x() - margin;
            }
        }

        float leftGrowCapacity = numLeftGrow > 0 ? (getFormattedSize().x - leftPointer) / numLeftGrow : 0;
        float centerGrowCapacity = numCenterGrow > 0 ? (getFormattedSize().x - centerPointer) * 2 / numCenterGrow : 0;
        float rightGrowCapacity = numRightGrow > 0 ? (getFormattedSize().x - (getFormattedSize().x - rightPointer)) / numRightGrow : 0;

        leftPointer = 0;

        //Place children ignoring vertical preference and adjust cursors
        for (int i = 0; i < getChildren().size(); i++) {
            Node<?> child = getChildren().get(i);

            if (child.getScaleMode() == ScaleMode.GROW) {
                child.getFormattedSize().set(
                        switch (child.getAlignment()) {
                            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> leftGrowCapacity;
                            case TOP, CENTER, BOTTOM -> centerGrowCapacity;
                            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> rightGrowCapacity;
                            case FIXED_POSITION -> child.getFormattedSize().x();
                        },
                        getFormattedSize().y
                );
                child.setComputedFixedSize(true);
            }
            child.layout();

            if (child.getAlignment() == Node.Alignment.FIXED_POSITION) continue;
            Vector2f newPos = getPreferredNodePosition(getFormattedSize(), child).mul(0, 1);

            child.setPosition(newPos.add(
                    switch (child.getAlignment()) {
                        case FIXED_POSITION -> 0;
                        case TOP, TOP_LEFT, TOP_RIGHT -> leftPointer;
                        case CENTER, CENTER_LEFT, CENTER_RIGHT -> centerPointer;
                        case BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> rightPointer;
                    }, 0));

            float margin = 0;
            Node<?> nextChild;
            if (getChildren().size() - 1 > i && (nextChild = getChildren().get(i + 1)) != null) {
                margin = Math.max(nextChild.getMargin().z(), child.getMargin().w());
            }

            float childWidth = child.getFormattedSize().x();
            switch (child.getAlignment()) {
                case TOP, TOP_LEFT, TOP_RIGHT -> leftPointer += childWidth + margin;
                case CENTER, CENTER_LEFT, CENTER_RIGHT -> centerPointer += childWidth + margin;
                case BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> rightPointer += childWidth + margin;
            }
        }

        if (getChildren().stream().anyMatch(child -> !child.getComputedFixedSize()))
            throw new IllegalStateException("Absolute size could not be computed for all children");
    }

}
