package org.etieskrill.engine.graphics.model;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

@Getter
public class Vertex {

    private final Vector3fc position;
    private final @Nullable Vector3fc normal;
    private final @Nullable Vector2fc textureCoords;
    private final @Nullable Vector3fc tangent;
    private final @Nullable Vector3fc biTangent;
    private final Vector4ic bones;
    private final Vector4fc boneWeights;

    Vertex(Vector3fc position, @Nullable Vector3fc normal, @Nullable Vector2fc textureCoords, @Nullable Vector3fc tangent, @Nullable Vector3fc biTangent, Vector4ic bones, Vector4fc boneWeights) {
        this.position = position;
        this.normal = normal;
        this.textureCoords = textureCoords;
        this.tangent = tangent;
        this.biTangent = biTangent;
        this.bones = bones;
        this.boneWeights = boneWeights;
    }

    public static VertexBuilder builder(Vector3fc position) {
        return new VertexBuilder().position(position);
    }

    public static class VertexBuilder {
        private @Accessors(fluent = true)
        @Getter Vector4i bones = new Vector4i(-1);
        private @Accessors(fluent = true)
        @Getter Vector4f boneWeights = new Vector4f(-1);
        private Vector3fc position;
        private @Nullable Vector3fc normal;
        private @Nullable Vector2fc textureCoords;
        private @Nullable Vector3fc tangent;
        private @Nullable Vector3fc biTangent;

        VertexBuilder() {
        }

        public VertexBuilder position(Vector3fc position) {
            this.position = position;
            return this;
        }

        public VertexBuilder normal(@Nullable Vector3fc normal) {
            this.normal = normal;
            return this;
        }

        public VertexBuilder textureCoords(@Nullable Vector2fc textureCoords) {
            this.textureCoords = textureCoords;
            return this;
        }

        public VertexBuilder tangent(@Nullable Vector3fc tangent) {
            this.tangent = tangent;
            return this;
        }

        public VertexBuilder biTangent(@Nullable Vector3fc biTangent) {
            this.biTangent = biTangent;
            return this;
        }

        public VertexBuilder bones(Vector4i bones) {
            this.bones = bones;
            return this;
        }

        public VertexBuilder boneWeights(Vector4f boneWeights) {
            this.boneWeights = boneWeights;
            return this;
        }

        public Vertex build() {
            return new Vertex(this.position, this.normal, this.textureCoords, this.tangent, this.biTangent, this.bones, this.boneWeights);
        }

        public String toString() {
            return "Vertex.VertexBuilder(bones=" + this.bones + ", boneWeights=" + this.boneWeights + ", position=" + this.position + ", normal=" + this.normal + ", textureCoords=" + this.textureCoords + ", tangent=" + this.tangent + ", biTangent=" + this.biTangent + ")";
        }
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
