package io.github.etieskrill.injection.extension.shader.reflection

import io.github.etieskrill.injection.extension.shader.AbstractShader
import io.github.etieskrill.injection.extension.shader.Texture
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

//TODO probably allow rewiring a method body in AbstractShader via reflection or bytecode manip to pre-register uniforms
class UniformDelegate<T : Any> : ReadWriteProperty<AbstractShader, T> {
    private var initialised = false

    override operator fun getValue(thisRef: AbstractShader, property: KProperty<*>): T =
        TODO()

    override operator fun setValue(thisRef: AbstractShader, property: KProperty<*>, value: T) {
        if (!initialised) {
            thisRef.addUniform(property.name, value::class.java)
            initialised = true
        }
        when (value) {
            is Texture -> thisRef.setTexture(property.name, value)
            else -> thisRef.setUniform(property.name, value)
        }
    }
}

fun <T : Any> uniform() = UniformDelegate<T>()

class ArrayUniformDelegate<T : Any>(private val size: Int) : ReadWriteProperty<AbstractShader, Array<T>> {
    private var initialised = false
    override fun getValue(thisRef: AbstractShader, property: KProperty<*>): Array<T> =
        TODO()

    override fun setValue(thisRef: AbstractShader, property: KProperty<*>, value: Array<T>) {
        require(size == value.size)
        if (!initialised) {
            thisRef.addUniformArray(property.name, size, value::class.java)
            initialised = true
        }
        thisRef.setUniformArray(property.name, value as Array<Any>) //FIXME wut? T is upper-bounded by Any though, so why does T not upcast to Any?
    }
}

fun <T : Any> arrayUniform(size: Int) = ArrayUniformDelegate<T>(size)

class UniformName : ReadOnlyProperty<Any?, String> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): String =
        property.name.removeSuffix("Name")
}

fun uniformName() = UniformName()
