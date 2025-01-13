package org.etieskrill.game.horde

import io.github.etieskrill.injection.extension.shaderreflection.ReflectShader
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram

@ReflectShader
class FloorShader : ShaderProgram(listOf("Floor.glsl"))

@ReflectShader
class BlitShader : ShaderProgram(listOf("Blit.glsl"))
