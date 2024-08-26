package org.etieskrill.engine.graphics.model;

import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.nio.ByteBuffer;

public class Vertex {

    public static final int
            POSITION_COMPONENTS = 3,
            NORMAL_COMPONENTS = 3,
            TEXTURE_COMPONENTS = 2,
            TANGENT_COMPONENTS = 3,
            BITANGENT_COMPONENTS = 3,
            BONE_COMPONENTS = 4,
            BONE_WEIGHT_COMPONENTS = 4,
            COMPONENTS = POSITION_COMPONENTS + NORMAL_COMPONENTS + TEXTURE_COMPONENTS + TANGENT_COMPONENTS
                    + BITANGENT_COMPONENTS + BONE_COMPONENTS + BONE_WEIGHT_COMPONENTS;

    public static final int
            POSITION_BYTES = POSITION_COMPONENTS * Float.BYTES,
            NORMAL_BYTES = NORMAL_COMPONENTS * Float.BYTES,
            TEXTURE_BYTES = TEXTURE_COMPONENTS * Float.BYTES,
            TANGENT_BYTES = TANGENT_COMPONENTS * Float.BYTES,
            BITANGENT_BYTES = BITANGENT_COMPONENTS * Float.BYTES,
            BONE_BYTES = BONE_COMPONENTS * Integer.BYTES,
            BONE_WEIGHT_BYTES = BONE_WEIGHT_COMPONENTS * Float.BYTES,
            COMPONENT_BYTES = POSITION_BYTES + NORMAL_BYTES + TEXTURE_BYTES + TANGENT_BYTES + BITANGENT_BYTES
                    + BONE_BYTES + BONE_WEIGHT_BYTES;

    private final Vector3fc position;
    private final @Nullable Vector3fc normal;
    private final @Nullable Vector2fc textureCoords;
    private final @Nullable Vector3fc tangent;
    private final @Nullable Vector3fc biTangent;
    private final @Nullable Vector4ic bones;
    private final @Nullable Vector4fc boneWeights;

    //TODO WOOOOOOOOOOOOOOOOOOOOOOOO LOMBOOOOOOK WHEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEN - now
    public static class Builder {
        private Vector3fc position;
        private Vector3fc normal;
        private Vector2fc textureCoords;
        private Vector3fc tangent;
        private Vector3fc biTangent;
        private final Vector4i bones = new Vector4i(-1);
        private final Vector4f boneWeights = new Vector4f(-1);

        public Builder(Vector3fc position) {
            this.position = position;
        }

        public Vector3fc position() {
            return position;
        }

        public Builder position(Vector3fc position) {
            this.position = position;
            return this;
        }

        public Builder normal(Vector3fc normal) {
            this.normal = normal;
            return this;
        }

        public Vector2fc textureCoords() {
            return textureCoords;
        }

        public Builder textureCoords(Vector2fc textureCoords) {
            this.textureCoords = textureCoords;
            return this;
        }

        public Vector3fc tangent() {
            return tangent;
        }

        public Builder tangent(Vector3fc tangent) {
            this.tangent = tangent;
            return this;
        }

        public Vector3fc biTangent() {
            return biTangent;
        }

        public Builder biTangent(Vector3fc biTangent) {
            this.biTangent = biTangent;
            return this;
        }

        public Vector4i bones() {
            return bones;
        }

        public Builder bones(Vector4i bones) {
            this.bones.set(bones);
            return this;
        }

        public Vector4f boneWeights() {
            return boneWeights;
        }

        public Builder boneWeights(Vector4f boneWeights) {
            this.boneWeights.set(boneWeights);
            return this;
        }

        public Vertex build() {
            return new Vertex(position, normal, textureCoords, tangent, biTangent, bones, boneWeights);
        }
    }

    public Vertex(Vector3fc position, @Nullable Vector3fc normal, @Nullable Vector2fc textureCoords) {
        this(position, normal, textureCoords, null, null, null, null);
    }

    public Vertex(Vector3fc position,
                  @Nullable Vector3fc normal, @Nullable Vector2fc textureCoords,
                  @Nullable Vector3fc tangent, @Nullable Vector3fc biTangent,
                  @Nullable Vector4ic bones, @Nullable Vector4fc boneWeights) {
        this.position = position;
        this.normal = normal;
        this.textureCoords = textureCoords;
        this.tangent = tangent;
        this.biTangent = biTangent;
        this.bones = bones;
        this.boneWeights = boneWeights;
    }

    public Vector3fc getPosition() {
        return position;
    }

    public @Nullable Vector3fc getNormal() {
        return normal;
    }

    public @Nullable Vector2fc getTextureCoords() {
        return textureCoords;
    }

    public @Nullable Vector3fc getTangent() {
        return tangent;
    }

    public @Nullable Vector3fc getBiTangent() {
        return biTangent;
    }

    public @Nullable Vector4ic getBones() {
        return bones;
    }

    public @Nullable Vector4fc getBoneWeights() {
        return boneWeights;
    }

    public void buffer(ByteBuffer buffer) {
        //TODO debug: write this in blood into a general debugging guide
        //buffers created by java's nio package, whether wrapping or direct (e.g. ByteBuffer.allocateDirect(...), are
        //allocated on-heap, which is obvious for the java-native array wrapping buffers, and is true for direct
        //buffers as well. LWJGL operations (at least in opengl) such as writing buffers require specific off-heap
        //buffers, which are created using the BufferUtils class provided by LWJGL. if an on-heap buffer is passed to
        //any of the library's operations, the data will most likely have been overwritten already, as it is considered
        //freed or something by the jvm, yet can still be read by the native operations. this in and of itself is a
        //lapse in judgement by whomever decided to even allow regular nio buffers to be used in the library's methods.
        //TODO maybe an annotation or *something* could be leveraged to help identify such errors

        if (buffer.remaining() < COMPONENT_BYTES)
            throw new IllegalArgumentException("Buffer does not have enough space left for a vertex");

        put(buffer, position);

        if (normal != null) put(buffer, normal);
        else putZero(buffer, NORMAL_BYTES);

        if (textureCoords != null) put(buffer, textureCoords);
        else putZero(buffer, TEXTURE_BYTES);

        if (tangent != null) {
            put(buffer, tangent);
            put(buffer, biTangent);
        } else putZero(buffer, TANGENT_BYTES + BITANGENT_BYTES);

        if (bones != null) put(buffer, bones);
        else buffer.putInt(-1).putInt(-1).putInt(-1).putInt(-1);

        if (boneWeights != null) put(buffer, boneWeights);
        else putZero(buffer, BONE_WEIGHT_BYTES);
    }

    private void put(ByteBuffer buffer, Object value) {
        switch (value) {
            case Vector2fc vec2 -> vec2.get(buffer).position(buffer.position() + 2 * Float.BYTES);
            case Vector3fc vec3 -> vec3.get(buffer).position(buffer.position() + 3 * Float.BYTES);
            case Vector4ic vec4i -> vec4i.get(buffer).position(buffer.position() + 4 * Integer.BYTES);
            case Vector4fc vec4 -> vec4.get(buffer).position(buffer.position() + 4 * Float.BYTES);
            default -> throw new IllegalArgumentException("Unexpected vertex attribute type: " + value.getClass().getSimpleName());
        }
    }

    private static void putZero(ByteBuffer block, int numBytes) {
        for (int i = 0; i < numBytes; i++) block.put((byte) 0);
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
