package org.etieskrill.engine.graphics.data

import net.bifreus.kml.Vec3
import org.etieskrill.engine.graphics.shader.UniformMappable
import org.etieskrill.engine.graphics.shader.UniformMapper

data class DirectionalLight(
    val direction: Vec3,

    val ambient: Vec3 = Vec3(1f),
    val diffuse: Vec3 = Vec3(1f),
    val specular: Vec3 = Vec3(1f),
) : UniformMappable {

    @Suppress("KotlinConstantConditions")
    override fun map(mapper: UniformMapper): Boolean = mapper.run {
        map("direction", direction)
        map("ambient", ambient)
        map("diffuse", diffuse)
        map("specular", specular)
        true
    }

}
