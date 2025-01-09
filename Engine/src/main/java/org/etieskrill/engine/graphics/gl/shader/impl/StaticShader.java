package org.etieskrill.engine.graphics.gl.shader.impl;

import io.etieskrill.injection.extension.shaderreflection.ReflectShader;
import org.etieskrill.engine.graphics.data.DirectionalLight;
import org.etieskrill.engine.graphics.data.PointLight;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.joml.Vector2fc;
import org.joml.Vector3fc;

import java.util.List;

@ReflectShader(files = {"Phong.vert", "Phong.frag"}) //TODO also extract superclass, supress with flag
public class StaticShader extends ShaderProgram {

    public StaticShader() {
        super(List.of("Phong.vert", "Phong.frag"));

//        uniform("viewPosition", VEC3),
//                uniform("textureScale", VEC2, new Vector2f(1f)),
//
//                uniform("blinnPhong", BOOLEAN, true),
//
//                uniformArray("globalLights", 1, STRUCT),
//                uniformArray("lights", 2, STRUCT),
//
////                    addUniform("globalShadowMap", SAMPLER2D),
//                uniform("hasShadowMap", BOOLEAN, false),
//                uniform("shadowMap", SAMPLER2D),
//                uniform("hasPointShadowMaps", BOOLEAN, false),
//                uniform("pointShadowMaps", SAMPLER_CUBE_MAP_ARRAY),
//
//                uniform("pointShadowFarPlane", FLOAT, 20f)
        //TODO readd defaults by setting via generated accessors
    }

    public void setTextureScale(Vector2fc textureScale) {
        setUniform("textureScale", textureScale);
    }

    public void setBlinnPhong(boolean blinnPhong) {
        setUniform("blinnPhong", blinnPhong);
    }

    public void setViewPosition(Vector3fc viewPosition) {
        setUniform("viewPosition", viewPosition);
    }

    public void setGlobalLights(DirectionalLight... lights) {
        setUniformArray("globalLights", lights);
    }

    public void setLights(PointLight[] pointLights) {
        setUniformArray("lights", pointLights);
    }

    public void setPointShadowFarPlane(float farPlane) {
        setUniform("pointShadowFarPlane", farPlane);
    }
}
