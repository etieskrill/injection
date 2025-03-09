package org.etieskrill.engine

import org.etieskrill.engine.graphics.gl.shader.ShaderProgram

class TestShader : ShaderProgram(
    listOf("Test.glsl")
)