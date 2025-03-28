package io.github.etieskrill.injection.extension.shader.dsl

import io.github.etieskrill.injection.extension.shader.AbstractShader
import io.github.etieskrill.injection.extension.shader.Texture

class Shader : AbstractShader {

    override fun setUniform(name: String, value: Any) = error("Should not be called in testing context")
    override fun setUniformArray(name: String, value: Array<Any>) = error("Should not be called in testing context")
    override fun setUniformArray(name: String, index: Int, value: Any) =
        error("Should not be called in testing context")

    override fun setTexture(name: String, texture: Texture) = error("Should not be called in testing context")
    override fun addUniform(name: String, type: Class<*>) = error("Should not be called in testing context")
    override fun addUniformArray(name: String, size: Int, type: Class<*>) =
        error("Should not be called in testing context")

    override fun start(): Unit = error("Should not be called in testing context")
    override fun stop(): Unit = error("Should not be called in testing context")

    override fun dispose(): Unit = error("Should not be called in testing context")

}
