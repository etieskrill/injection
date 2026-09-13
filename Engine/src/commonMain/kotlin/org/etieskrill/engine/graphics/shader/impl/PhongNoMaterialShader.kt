package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader
import org.etieskrill.engine.graphics.shader.Shader
import org.joml.Matrix3f
import org.joml.Matrix4f

@ReflectShader
class PhongNoMaterialShader : Shader(listOf("PhongNoMaterial.glsl"), false) {
    init {
        addUniform("mesh", Matrix4f::class)
        setUniform("mesh", Matrix4f())
        addUniform("model", Matrix4f::class)
        setUniform("model", Matrix4f())
        addUniform("normal", Matrix3f::class)
        setUniform("normal", Matrix3f())
        addUniform("combined", Matrix4f::class)
        setUniform("combined", Matrix4f())
        addUniform("lightCombined", Matrix4f::class)
        setUniform("lightCombined", Matrix4f())
    }
}
