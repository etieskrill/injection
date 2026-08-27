package org.etieskrill.engine.graphics.data

import org.etieskrill.engine.graphics.shader.UniformMappable
import org.etieskrill.engine.graphics.shader.UniformMapper
import org.joml.Vector3f

data class PointLight(
    val position: Vector3f,

    private val ambient: Vector3f,
    private val diffuse: Vector3f,
    private val specular: Vector3f,

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
