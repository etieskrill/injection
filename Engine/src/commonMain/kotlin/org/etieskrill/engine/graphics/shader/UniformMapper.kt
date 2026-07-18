package org.etieskrill.engine.graphics.shader

import io.github.etieskrill.injection.extension.shader.AbstractShader
import io.github.etieskrill.injection.extension.shader.Texture

object UniformMapper {
    private var shader: AbstractShader? = null
    private var structName: String? = null

    fun get(shader: AbstractShader, structName: String): UniformMapper {
        this.shader = shader
        this.structName = structName
        return this
    }

    /**
     * Maps a `texture` to a struct uniform sampler named `name`. `null` values are ignored.
     *
     * @param name    uniform struct member name
     * @param texture texture to sample
     * @return the [UniformMapper] for chaining
     */
    fun map(name: String, texture: Texture?): UniformMapper {
        if (texture != null) shader!!.setTexture("$structName.$name", texture)
        return this
    }

    /**
     * Maps a field `value` to a struct uniform member named `name`. `null` values are ignored.
     *
     * @param name  uniform struct member name
     * @param value uniform value
     * @return the [UniformMapper] for chaining
     */
    fun map(name: String, value: Any?): UniformMapper {
        //TODO strict nested uniforms: probably hook into here for registration
        if (value != null) shader!!.setUniform("$structName.$name", value)
        return this
    }
}
