package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader
import net.bifreus.kml.Mat4
import net.bifreus.kml.Vec4
import org.etieskrill.engine.graphics.shader.Shader

@ReflectShader
class SingleColourShader : Shader(listOf("shaders/SingleColour.vert", "shaders/SingleColour.frag")) {
    init {
        addUniform("model", Mat4::class)
        addUniform("combined", Mat4::class)
        addUniform("colour", Vec4::class)
    }
}
