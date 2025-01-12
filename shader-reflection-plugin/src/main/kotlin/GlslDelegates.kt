package io.github.etieskrill.injection.extension.shaderreflection

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

class ArrayUniformDelegate<T : Any>(private val size: Int) : ReadWriteProperty<AbstractShader, Array<T>> {
    override fun getValue(thisRef: AbstractShader, property: KProperty<*>): Array<T> =
        TODO()

    override fun setValue(thisRef: AbstractShader, property: KProperty<*>, value: Array<T>) {
        require(size == value.size)
        thisRef.setUniformArray(property.name, value as Array<Any>) //FIXME wut? T is upper-bounded by Any though, so why does T not upcast to Any?
    }
}

fun <T : Any> arrayUniform(size: Int) = ArrayUniformDelegate<T>(size)
