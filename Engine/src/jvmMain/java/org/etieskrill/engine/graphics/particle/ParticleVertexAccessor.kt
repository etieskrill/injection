package org.etieskrill.engine.graphics.particle

import org.etieskrill.engine.graphics.buffer.VertexArrayAccessor
import org.joml.Matrix2f
import org.joml.Vector3f
import org.joml.Vector4f

object ParticleVertexAccessor : VertexArrayAccessor<Particle>() {
    override fun registerFields() {
        addField<Vector3f> { particle, buffer -> buffer += particle.position }
        addField<Matrix2f> { particle, buffer -> buffer += particle.transform }
        addField<Vector4f> { particle, buffer -> buffer += particle.colour }
    }
}
