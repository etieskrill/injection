package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader
import net.bifreus.kml.Mat4
import net.bifreus.kml.Vec2
import org.etieskrill.engine.graphics.shader.Shader
import org.etieskrill.engine.graphics.texture.ArrayTexture2D

@ReflectShader
class TextShader : Shader(listOf("shaders/Text.glsl")) {
    init {
        addUniform("glyphTextureSize", Vec2::class)
        addUniform("combined", Mat4::class)
        addUniform("glyphs", ArrayTexture2D::class)
    }
}
