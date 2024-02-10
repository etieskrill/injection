package org.etieskrill.engine.graphics.model;

import org.joml.Vector2fc;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4i;

import java.nio.ByteBuffer;
import java.util.List;

public class Vertex {
    
    public static final int
            POSITION_COMPONENTS = 3,
            NORMAL_COMPONENTS = 3,
            TEXTURE_COMPONENTS = 2,
            BONE_COMPONENTS = 4,
            BONE_WEIGHT_COMPONENTS = 4,
            COMPONENTS = POSITION_COMPONENTS + NORMAL_COMPONENTS + TEXTURE_COMPONENTS + BONE_COMPONENTS + BONE_WEIGHT_COMPONENTS;

    public static final int COMPONENT_BYTES =
            POSITION_COMPONENTS * Float.BYTES +
                    NORMAL_COMPONENTS * Float.BYTES +
                    TEXTURE_COMPONENTS * Float.BYTES +
                    BONE_COMPONENTS * Integer.BYTES +
                    BONE_WEIGHT_COMPONENTS * Float.BYTES;

    private final Vector3fc position;
    private Vector3fc normal;
    private Vector2fc textureCoords;
    private Vector4i bones;
    private Vector4f boneWeights;

    public Vertex(Vector3fc position, Vector3fc normal, Vector2fc textureCoords) {
        this.position = position;
        this.normal = normal;
        this.textureCoords = textureCoords;
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

    public List<Float> toList() { //TODO optimise this
        return List.of(
                position.x(), position.y(), position.z(),
                normal.x(), normal.y(), normal.z(),
                textureCoords.x(), textureCoords.y()
        );
    }

    public ByteBuffer block() {
        ByteBuffer block = ByteBuffer.allocateDirect(COMPONENT_BYTES);
        block.putFloat(position.x()).putFloat(position.y()).putFloat(position.z());
        if (normal != null) block.putFloat(normal.x()).putFloat(normal.y()).putFloat(normal.z());
        else block.position(block.position() + NORMAL_COMPONENTS * Float.BYTES);
        if (textureCoords != null) block.putFloat(textureCoords.x()).putFloat(textureCoords.y());
        else block.position(block.position() + TEXTURE_COMPONENTS * Float.BYTES);
        if (bones != null) block.putInt(bones.x()).putInt(bones.y()).putInt(bones.z()).putInt(bones.w());
        else block.position(block.position() + BONE_COMPONENTS * Integer.BYTES);
        if (boneWeights != null)
            block.putFloat(boneWeights.x()).putFloat(boneWeights.y()).putFloat(boneWeights.z()).putFloat(boneWeights.w());
        else block.position(block.position() + BONE_WEIGHT_COMPONENTS * Float.BYTES);
        block.rewind();
        return block;
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
