package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader
import org.etieskrill.engine.graphics.shader.Shader
import org.etieskrill.engine.graphics.texture.ArrayTexture2D
import org.joml.Matrix4f
import org.joml.Vector2f

@ReflectShader
class TextShader : Shader(listOf("shaders/Text.glsl")) {
    init {
        addUniform("glyphTextureSize", Vector2f::class)
        addUniform("combined", Matrix4f::class)
        addUniform("glyphs", ArrayTexture2D::class)
    }
}
