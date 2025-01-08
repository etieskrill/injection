package io.etieskrill.injection.extension.shaderreflection

import io.github.etieskrill.extension.shaderreflection.AbstractShader
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class UniformDelegate<T : Any> : ReadWriteProperty<AbstractShader, T> {
    override operator fun getValue(shader: AbstractShader, property: KProperty<*>): T =
        TODO()

    override operator fun setValue(shader: AbstractShader, property: KProperty<*>, value: T) =
        shader.setUniform(property.name, value)
}

fun <T : Any> uniform() = UniformDelegate<T>()
