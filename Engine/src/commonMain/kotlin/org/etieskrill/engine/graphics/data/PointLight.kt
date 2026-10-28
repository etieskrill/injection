package org.etieskrill.engine.graphics.data

import net.bifreus.kml.Vec3
import org.etieskrill.engine.graphics.shader.UniformMappable
import org.etieskrill.engine.graphics.shader.UniformMapper

data class PointLight(
    val position: Vec3,

    private val ambient: Vec3,
    private val diffuse: Vec3,
    private val specular: Vec3,

    private val constant: Float,
    private val linear: Float,
    private val quadratic: Float
) : UniformMappable {

    @Suppress("KotlinConstantConditions")
    override fun map(mapper: UniformMapper): Boolean = mapper.run {
        map("position", position)
        map("ambient", ambient)
        map("diffuse", diffuse)
        map("specular", specular)
        map("constant", constant)
        map("linear", linear)
        map("quadratic", quadratic)
        true
    }

}
