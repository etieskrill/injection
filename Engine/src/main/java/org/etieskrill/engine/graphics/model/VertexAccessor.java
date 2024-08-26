package org.etieskrill.engine.graphics.model;

import lombok.Getter;
import org.etieskrill.engine.graphics.gl.VertexArrayAccessor;
import org.joml.Vector2fc;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.joml.Vector4ic;

public class VertexAccessor extends VertexArrayAccessor<Vertex> {

    private static final @Getter VertexAccessor instance = new VertexAccessor();

    @Override
    protected void registerFields() {
        addField(Vector3fc.class, (vertex, buffer) -> vertex.getPosition().get(buffer));
        addField(Vector3fc.class, (vertex, buffer) -> {
            if (vertex.getNormal() != null) vertex.getNormal().get(buffer);
            else buffer.putFloat(0).putFloat(0).putFloat(0);
        }, true);
        addField(Vector2fc.class, (vertex, buffer) -> {
            if (vertex.getTextureCoords() != null) vertex.getTextureCoords().get(buffer);
            else buffer.putFloat(0).putFloat(0);
        });
        addField(Vector3fc.class, (vertex, buffer) -> {
            if (vertex.getTangent() != null) vertex.getTangent().get(buffer);
            else buffer.putFloat(0).putFloat(0).putFloat(0);
        }, true);
        addField(Vector3fc.class, (vertex, buffer) -> {
            if (vertex.getBiTangent() != null) vertex.getBiTangent().get(buffer);
            else buffer.putFloat(0).putFloat(0).putFloat(0);
        }, true);
        addField(Vector4ic.class, (vertex, buffer) -> vertex.getBones().get(buffer));
        addField(Vector4fc.class, (vertex, buffer) -> vertex.getBoneWeights().get(buffer));
    }

}
