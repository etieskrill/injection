package org.etieskrill.engine.graphics.data

import org.etieskrill.engine.graphics.shader.UniformMappable
import org.etieskrill.engine.graphics.shader.UniformMapper
import org.joml.Vector3f

data class DirectionalLight(
    val direction: Vector3f,

    val ambient: Vector3f = Vector3f(1f),
    val diffuse: Vector3f = Vector3f(1f),
    val specular: Vector3f = Vector3f(1f),
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
