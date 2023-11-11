package org.etieskrill.engine.scene.component;

import glm_.vec2.Vec2;
import glm_.vec4.Vec4;

public class LayoutUtils {

    static Vec2 getMinNodeSize(Node node) {
        Vec4 margin = node.getMargin();
        return node.getSize().plus(
                new Vec2(margin.getZ() + margin.getW(), margin.getX() + margin.getY())
        );
    }

    static Vec2 getPreferredNodePosition(Vec2 size, Node node) {
        Vec2 nodeSize = node.getSize().plus(
                new Vec2(node.getMargin().getW(), node.getMargin().getY())
                .times(2)
        );
        
        Vec2 marginPos = new Vec2(
                node.getMargin().getZ(),
                node.getMargin().getX()
        );
        
        Vec2 pos = switch (node.getAlignment()) {
            case TOP_LEFT -> new Vec2(0f, 0f);
            case TOP -> size.minus(nodeSize).times(0.5f, 0f);
            case TOP_RIGHT -> size.minus(nodeSize).times(1f, 0f);
            case CENTER_LEFT -> size.minus(nodeSize).times(0f, 0.5f);
            case CENTER -> size.times(0.5f).minus(nodeSize.times(0.5f));
            case CENTER_RIGHT -> size.minus(nodeSize).times(1f, 0.5f);
            case BOTTOM_LEFT -> size.minus(nodeSize).times(0f, 1f);
            case BOTTOM -> size.minus(nodeSize).times(0.5f, 1f);
            case BOTTOM_RIGHT -> size.minus(nodeSize);
        };
        
        return pos.plus(marginPos);
    }
    
}
