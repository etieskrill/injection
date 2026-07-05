package org.etieskrill.engine.graphics.particle

import org.etieskrill.engine.graphics.gl.VertexArrayAccessor
import org.joml.Matrix2f
import org.joml.Vector3f
import org.joml.Vector4f

object ParticleVertexAccessor : VertexArrayAccessor<Particle>() {
    override fun registerFields() {
        addField<Vector3f> { particle, buffer -> particle.position[buffer] }
        addField<Matrix2f> { particle, buffer -> particle.transform[buffer] }
        addField<Vector4f> { particle, buffer -> particle.colour[buffer] }
    }
}
