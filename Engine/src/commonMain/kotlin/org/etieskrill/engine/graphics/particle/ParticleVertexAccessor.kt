package org.etieskrill.engine.graphics.particle

import net.bifreus.kml.Mat2
import net.bifreus.kml.Vec3
import net.bifreus.kml.Vec4
import org.etieskrill.engine.graphics.buffer.VertexArrayAccessor

object ParticleVertexAccessor : VertexArrayAccessor<Particle>() {
    override fun registerFields() {
        addField<Vec3> { particle, buffer -> buffer += particle.position }
        addField<Mat2> { particle, buffer -> buffer += particle.transform }
        addField<Vec4> { particle, buffer -> buffer += particle.colour }
    }
}
