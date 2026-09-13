package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader
import org.etieskrill.engine.graphics.shader.Shader
import org.joml.Matrix4f
import org.joml.Vector4f

@ReflectShader
class SingleColourShader : Shader(listOf("shaders/SingleColour.vert", "shaders/SingleColour.frag")) {
    init {
        addUniform("model", Matrix4f::class)
        addUniform("combined", Matrix4f::class)
        addUniform("colour", Vector4f::class)
    }
}
