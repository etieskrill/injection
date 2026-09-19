package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader
import org.etieskrill.engine.graphics.shader.Shader
import org.joml.Matrix4f

@ReflectShader(files = ["Phong.vert", "Phong.frag"]) //TODO also extract superclass, supress with flag
class StaticShader : Shader(listOf("shaders/Phong.vert", "shaders/Phong.frag")) {
    init {
        addUniform("combined", Matrix4f::class)
//        setTextureScale(this, new Vector2f(1f))
//
//        setBlinnPhong(this, true)
//
//        setHasShadowMap(this, false)
//        setHasPointShadowMaps(this, false)
//
//        setPointShadowFarPlane(this, 20f)
//
//        setUniform("material.colourDiffuse", new Vector4f(1f), false)
    }
}
