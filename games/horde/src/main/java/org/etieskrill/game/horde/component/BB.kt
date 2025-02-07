package org.etieskrill.game.horde.component

import org.etieskrill.engine.graphics.gl.shader.ShaderProgram.UniformMapper
import org.etieskrill.engine.graphics.gl.shader.UniformMappable
import org.etieskrill.engine.graphics.texture.Texture2D
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

data class BB(
    val sprite: Texture2D,
    var size: Vector2f,
    val offset: Vector3f = Vector3f(),
    var rotation: Float = 0f,
    val punchThrough: Boolean = false
) : UniformMappable {
//    override fun map(mapper: ShaderProgram.UniformMapper?): Boolean {
//        mapper!!
//            .map("sprite", sprite)
//            .map("size", size)
//            .map("offset", offset)
//            .map("rotation", rotation)
//            .map("punchThrough", punchThrough)
//        return true
//    }
    override fun map(mapper: UniformMapper?): Boolean = mapper!!.map {
        "sprite" to sprite
        "size" to size
        "offset" to offset
        "rotation" to rotation
        "punchThrough" to punchThrough
    }
}

val UniformMapper.mapperContext: UniformMapperContext by LazyMapperContext()

inline fun UniformMapper.map(init: UniformMapperContext.() -> Unit): Boolean {
    init(mapperContext)
    return true
}

class UniformMapperContext(private val mapper: UniformMapper) {
    infix fun String.to(value: Any): UniformMapper = mapper.map(this, value)
}

class LazyMapperContext : ReadOnlyProperty<UniformMapper, UniformMapperContext> {
    private lateinit var instance: UniformMapperContext

    override fun getValue(thisRef: UniformMapper, property: KProperty<*>): UniformMapperContext {
        if (!this::instance.isInitialized) {
            instance = UniformMapperContext(thisRef)
        }
        return instance
    }
}
