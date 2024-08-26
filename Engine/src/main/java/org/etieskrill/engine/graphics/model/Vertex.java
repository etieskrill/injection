package org.etieskrill.engine.graphics.model;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

@Getter
@Builder
public class Vertex {

    private final Vector3fc position;
    private final @Nullable Vector3fc normal;
    private final @Nullable Vector2fc textureCoords;
    private final @Nullable Vector3fc tangent;
    private final @Nullable Vector3fc biTangent;
    private final Vector4ic bones;
    private final Vector4fc boneWeights;

    public static VertexBuilder builder(Vector3fc position) {
        return new VertexBuilder().position(position);
    }

    public static class VertexBuilder {
        private @Accessors(fluent = true)
        @Getter Vector4i bones = new Vector4i(-1);
        private @Accessors(fluent = true)
        @Getter Vector4f boneWeights = new Vector4f(-1);
    }

    @Override
    public String toString() {
        return "Vertex{" +
                "position=" + position +
                ", normal=" + normal +
                ", textureCoords=" + textureCoords +
                ", bones=" + "(" + bones.x() + ", " + bones.y() + ", " + bones.z() + ", " + bones.w() + ")" +
                ", boneWeights=(%5.3f, %5.3f, %5.3f, %5.3f)".formatted(boneWeights.x(), boneWeights.y(), boneWeights.z(), boneWeights.w()) +
                '}';
    }

}
