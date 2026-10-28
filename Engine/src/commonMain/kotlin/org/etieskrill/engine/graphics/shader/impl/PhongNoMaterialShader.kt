package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader
import net.bifreus.kml.Mat3
import net.bifreus.kml.Mat4
import org.etieskrill.engine.graphics.shader.Shader

@ReflectShader
class PhongNoMaterialShader : Shader(listOf("PhongNoMaterial.glsl"), false) {
    init {
        addUniform("mesh", Mat4::class)
        setUniform("mesh", Mat4())
        addUniform("model", Mat4::class)
        setUniform("model", Mat4())
        addUniform("normal", Mat3::class)
        setUniform("normal", Mat3())
        addUniform("combined", Mat4::class)
        setUniform("combined", Mat4())
        addUniform("lightCombined", Mat4::class)
        setUniform("lightCombined", Mat4())
    }
}
