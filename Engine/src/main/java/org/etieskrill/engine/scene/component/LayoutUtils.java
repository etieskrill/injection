package org.etieskrill.engine.scene.component;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector4f;

public class LayoutUtils {

    @Contract("_ -> new")
    static Vector2f getMinNodeSize(Node<?> node) {
        Vector4f margin = node.getMargin();
        return new Vector2f(node.getSize())
                .add(margin.z() + margin.w(), margin.x() + margin.y());
    }

    @Contract("_, _ -> new")
    static Vector2f getPreferredNodePosition(@NotNull Vector2f size, @NotNull Node<?> node) {
        Vector2f nodeSize = new Vector2f(node.getSize()).add(
                new Vector2f(node.getMargin().w(), node.getMargin().y())
                .mul(2)
        );
        
        Vector2f marginPos = new Vector2f(
                node.getMargin().z(),
                node.getMargin().x()
        );

        Vector2f _size = new Vector2f(size);
        if (node.getAlignment() == Node.Alignment.FIXED_POSITION) {
            return node.getPosition();
        }
        Vector2f pos = switch (node.getAlignment()) {
            case FIXED_POSITION -> throw new IllegalStateException();

            case TOP_LEFT -> new Vector2f(0f, 0f);
            case TOP -> _size.sub(nodeSize).mul(0.5f, 0f);
            case TOP_RIGHT -> _size.sub(nodeSize).mul(1f, 0f);
            case CENTER_LEFT -> _size.sub(nodeSize).mul(0f, 0.5f);
            case CENTER -> _size.mul(0.5f).sub(nodeSize.mul(0.5f));
            case CENTER_RIGHT -> _size.sub(nodeSize).mul(1f, 0.5f);
            case BOTTOM_LEFT -> _size.sub(nodeSize).mul(0f, 1f);
            case BOTTOM -> _size.sub(nodeSize).mul(0.5f, 1f);
            case BOTTOM_RIGHT -> _size.sub(nodeSize);
        };
        
        return pos.add(marginPos);
    }
    
}
