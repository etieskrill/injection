package org.etieskrill.engine.entity

import org.etieskrill.engine.common.Disposable
import kotlin.reflect.KClass

class Entity(
    val id: Int
) : Disposable {

    private val mutableComponents = mutableMapOf<KClass<*>, Any>()
    val components: Map<KClass<*>, Any> = mutableComponents

    inline fun <reified T> getComponent() = components[T::class] as? T

    @Deprecated(message = "Use getComponent() instead.", replaceWith = ReplaceWith("getComponent<T>()"))
    fun <T : Any> getComponent(component: Class<T>) = components[component.kotlin] as? T

    fun <T : Any> addComponent(component: T): T {
        check(!mutableComponents.containsKey(component::class)) {
            "Entity already has component of type '${component::class.simpleName}'"
        }

        mutableComponents[component::class] = component
        return component
    }

    fun withComponent(component: Any): Entity {
        check(!mutableComponents.containsKey(component::class)) {
            "Entity already has component of type '${component::class.simpleName}'"
        }

        mutableComponents[component::class] = component
        return this
    }

    operator fun Any.unaryPlus() = addComponent(this)

    @Deprecated(message = "Use hasComponents<>() instead.", replaceWith = ReplaceWith("hasComponents<T>()"))
    fun hasComponents(vararg components: Class<*>) = this.components.keys.containsAll(components.map { it.kotlin })

    @JvmName(name = "hasComponents1")
    inline fun <reified T1> hasComponents() = components.contains(T1::class)

    @JvmName(name = "hasComponents2")
    inline fun <reified T1, reified T2> hasComponents() = components.keys.containsAll(listOf(T1::class, T2::class))

    @JvmName(name = "hasComponents3")
    inline fun <reified T1, reified T2, reified T3> hasComponents() =
        components.keys.containsAll(listOf(T1::class, T2::class, T3::class))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Entity) return false
        return id == other.id
    }

    override fun hashCode() = id

    override fun dispose() = components
        .values
        .filterIsInstance<Disposable>()
        .forEach { it.dispose() }

}
