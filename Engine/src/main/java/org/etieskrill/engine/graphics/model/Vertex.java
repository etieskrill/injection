package org.etieskrill.engine.graphics.model;

import org.joml.*;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

public class Vertex {
    
    public static final int
            POSITION_COMPONENTS = 3,
            NORMAL_COMPONENTS = 3,
            TEXTURE_COMPONENTS = 2,
            BONE_COMPONENTS = 4,
            BONE_WEIGHT_COMPONENTS = 4,
            COMPONENTS = POSITION_COMPONENTS + NORMAL_COMPONENTS + TEXTURE_COMPONENTS + BONE_COMPONENTS + BONE_WEIGHT_COMPONENTS;

    public static final int
            POSITION_BYTES = POSITION_COMPONENTS * Float.BYTES,
            NORMAL_BYTES = NORMAL_COMPONENTS * Float.BYTES,
            TEXTURE_BYTES = TEXTURE_COMPONENTS * Float.BYTES,
            BONE_BYTES = BONE_COMPONENTS * Integer.BYTES,
            BONE_WEIGHT_BYTES = BONE_WEIGHT_COMPONENTS * Float.BYTES,
            COMPONENT_BYTES = POSITION_BYTES + NORMAL_BYTES + TEXTURE_BYTES + BONE_BYTES + BONE_WEIGHT_BYTES;

    private final Vector3fc position;
    private Vector3fc normal;
    private Vector2fc textureCoords;
    private Vector4ic bones;
    private Vector4fc boneWeights;

    //TODO WOOOOOOOOOOOOOOOOOOOOOOOO LOMBOOOOOOK WHEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEN
    public static class Builder {
        private final Vector3fc position;
        private Vector3fc normal;
        private Vector2fc textureCoords;
        private final Vector4i bones = new Vector4i(-1);
        private final Vector4f boneWeights = new Vector4f(-1);

        public Builder(Vector3fc position) {
            this.position = position;
        }

        public Builder normal(Vector3fc normal) {
            this.normal = normal;
            return this;
        }

        public Builder textureCoords(Vector2fc textureCoords) {
            this.textureCoords = textureCoords;
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
            return new Vertex(position, normal, textureCoords, bones, boneWeights);
        }
    }

    public Vertex(Vector3fc position, Vector3fc normal, Vector2fc textureCoords) {
        this.position = position;
        this.normal = normal;
        this.textureCoords = textureCoords;
    }

    public Vertex(Vector3fc position, Vector3fc normal, Vector2fc textureCoords, Vector4ic bones, Vector4fc boneWeights) {
        this.position = position;
        this.normal = normal;
        this.textureCoords = textureCoords;
        this.bones = bones;
        this.boneWeights = boneWeights;
    }

    public Vector3fc getPosition() {
        return position;
    }

    public Vector3fc getNormal() {
        return normal;
    }

    public Vector2fc getTextureCoords() {
        return textureCoords;
    }

    public Vector4ic getBones() {
        return bones;
    }

    public Vector4fc getBoneWeights() {
        return boneWeights;
    }

    public ByteBuffer block() {
        //TODO debug: write this in blood into a general debugging guide
        //buffers created by java's nio package, whether wrapping or direct (e.g. ByteBuffer.allocateDirect(...), are
        //allocated on-heap, which is obvious for the java-native array wrapping buffers, and is true for direct
        //buffers as well. LWJGL operations (at least in opengl) such as writing buffers require specific off-heap
        //buffers, which are created using the BufferUtils class provided by LWJGL. if an on-heap buffer is passed to
        //any of the library's operations, the data will most likely have been overwritten already, as it is considered
        //freed or something by the jvm, yet can still be read by the native operations. this in and of itself is a
        //lapse in judgement by whomever decided to even allow regular nio buffers to be used in the library's methods.
        //TODO maybe an annotation or *something* could be leveraged to help identify such errors
        ByteBuffer block = BufferUtils.createByteBuffer(COMPONENT_BYTES);
        block.putFloat(position.x()).putFloat(position.y()).putFloat(position.z());
        if (normal != null) block.putFloat(normal.x()).putFloat(normal.y()).putFloat(normal.z());
        else putZero(block, NORMAL_BYTES);
        if (textureCoords != null) block.putFloat(textureCoords.x()).putFloat(textureCoords.y());
        else putZero(block, TEXTURE_BYTES);
        if (bones != null) block.putInt(bones.x()).putInt(bones.y()).putInt(bones.z()).putInt(bones.w());
        else block.putInt(-1).putInt(-1).putInt(-1).putInt(-1);
        if (boneWeights != null) block.putFloat(boneWeights.x()).putFloat(boneWeights.y()).putFloat(boneWeights.z()).putFloat(boneWeights.w());
        else putZero(block, BONE_WEIGHT_BYTES);
        return block.rewind();
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
                ", bones=" + bones +
                ", boneWeights=" + boneWeights +
                '}';
    }

}
