package org.etieskrill.engine.graphics.particle;

import lombok.Getter;
import org.etieskrill.engine.graphics.gl.VertexArrayAccessor;
import org.joml.Matrix2fc;
import org.joml.Vector3fc;
import org.joml.Vector4fc;

public class ParticleVertexAccessor extends VertexArrayAccessor<Particle> {

    public static final int BYTE_SIZE = (3 + 4 + 4) * Float.BYTES;

    @Getter
    private static final ParticleVertexAccessor instance = new ParticleVertexAccessor();

    @Override
    protected void registerFields() {
        addField(Vector3fc.class, Particle::getPosition);
        addField(Matrix2fc.class, Particle::getTransform);
        addField(Vector4fc.class, Particle::getColour);
    }

}
