package org.etieskrill.engine.graphics.particle;

import lombok.Getter;
import org.etieskrill.engine.graphics.gl.VertexArrayAccessor;
import org.joml.Matrix2fc;
import org.joml.Vector3fc;
import org.joml.Vector4fc;

public class ParticleVertexAccessor extends VertexArrayAccessor<Particle> {

    @Getter
    private static final ParticleVertexAccessor instance = new ParticleVertexAccessor();

    @Override
    protected void registerFields() {
        addField(Vector3fc.class, (particle, byteBuffer) -> particle.getPosition().get(byteBuffer));
        addField(Matrix2fc.class, (particle, byteBuffer) -> particle.getTransform().get(byteBuffer));
        addField(Vector4fc.class, (particle, byteBuffer) -> particle.getColour().get(byteBuffer));
    }

}
