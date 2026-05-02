package org.etieskrill.engine.scene.container;

import org.etieskrill.engine.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.util.List;

import static org.etieskrill.engine.scene.LayoutUtilsKt.getMinNodeSize;
import static org.etieskrill.engine.scene.LayoutUtilsKt.getPreferredNodePosition;

public class VBox extends Stack {

    public VBox() {
    }

    public VBox(@NotNull Node<?>... children) {
        super(List.of(children));
    }

    public VBox(@NotNull List<Node<?>> children) {
        super(children);
    }

    @Override
    protected void computeBoundingBox() {
        float width = 0, height = 0;

        for (Node<?> child : getChildren()) {
            var nodeSize = getMinNodeSize(child);
            height += nodeSize.y;
            width = Math.max(width, nodeSize.x);
        }

        getFormattedSize().set(width, height);
    }

    @Override
    public void layout() {
        if (!shouldFormat()) return;

        //Pre-calculate the size of the smallest fitting box around the children and position cursors accordingly
        float topPointer = 0, centerPointer = getFormattedSize().y() / 2, bottomPointer = getFormattedSize().y();
        float numTopGrow = 0, numCenterGrow = 0, numBottomGrow = 0;
        for (int i = 0; i < getChildren().size(); i++) {
            Node<?> child = getChildren().get(i);

            if (child.getScaleMode() == ScaleMode.GROW) {
                switch (child.getAlignment()) {
                    case TOP_LEFT, TOP, TOP_RIGHT -> numTopGrow++;
                    case CENTER, CENTER_LEFT, CENTER_RIGHT -> numCenterGrow++;
                    case BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> numBottomGrow++;
                }
                continue;
            }

            child.computeFixedSizes();

            if (child.getAlignment() == Alignment.FIXED_POSITION) continue;

            float margin = 0;
            Node<?> nextChild;
            if (getChildren().size() - 1 > i && (nextChild = getChildren().get(i + 1)) != null) {
                margin = Math.max(nextChild.getMargin().y(), child.getMargin().x());
            }

            switch (child.getAlignment()) {
                case TOP_LEFT, TOP, TOP_RIGHT -> topPointer += getMinNodeSize(child).y - margin;
                case CENTER, CENTER_LEFT, CENTER_RIGHT -> centerPointer -= child.getFormattedSize().y() / 2;
                case BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> bottomPointer -= getMinNodeSize(child).y() - margin;
            }
        }

        float topGrowCapacity = numTopGrow > 0 ? (getFormattedSize().y - topPointer) / numTopGrow : 0;
        float centerGrowCapacity = numCenterGrow > 0 ? (getFormattedSize().y - centerPointer) * 2 / numCenterGrow : 0;
        float bottomGrowCapacity = numBottomGrow > 0 ? (getFormattedSize().y - (getFormattedSize().y - bottomPointer)) / numBottomGrow : 0;

        topPointer = 0;

        //Place children ignoring vertical preference and adjust cursors
        for (int i = 0; i < getChildren().size(); i++) {
            Node<?> child = getChildren().get(i);

            if (child.getScaleMode() == ScaleMode.GROW) {
                child.getFormattedSize().set(
                        getFormattedSize().x,
                        switch (child.getAlignment()) {
                            case TOP_LEFT, TOP, TOP_RIGHT -> topGrowCapacity;
                            case CENTER_LEFT, CENTER, CENTER_RIGHT -> centerGrowCapacity;
                            case BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT -> bottomGrowCapacity;
                            case FIXED_POSITION -> child.getFormattedSize().y;
                        }
                );
                child.setComputedFixedSize(true);
            }
            child.layout();

            if (child.getAlignment() == Alignment.FIXED_POSITION) continue;
            Vector2f newPos = getPreferredNodePosition(getFormattedSize(), child).mul(1, 0);

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

            float childHeight = child.getFormattedSize().y();
            switch (child.getAlignment()) {
                case TOP, TOP_LEFT, TOP_RIGHT -> topPointer += childHeight + margin;
                case CENTER, CENTER_LEFT, CENTER_RIGHT -> centerPointer += childHeight + margin;
                case BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> bottomPointer += childHeight + margin;
            }
        }

        if (getChildren().stream().anyMatch(child -> !child.getComputedFixedSize()))
            throw new IllegalStateException("Absolute size could not be computed for all children");
    }

}
