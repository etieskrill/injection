package io.etieskrill.injection.extension.shaderreflection

import io.github.etieskrill.extension.shaderreflection.AbstractShader
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class UniformDelegate<T : Any> : ReadWriteProperty<AbstractShader, T> {
    override operator fun getValue(thisRef: AbstractShader, property: KProperty<*>): T =
        TODO()

    override operator fun setValue(thisRef: AbstractShader, property: KProperty<*>, value: T) =
        thisRef.setUniform(property.name, value)
}

//TODO register uniforms here
fun <T : Any> uniform() = UniformDelegate<T>()

class UniformName : ReadOnlyProperty<Any?, String> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): String =
        property.name.removeSuffix("Name")
}

fun uniformName() = UniformName()
