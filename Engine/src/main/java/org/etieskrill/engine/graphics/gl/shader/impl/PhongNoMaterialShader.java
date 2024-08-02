package org.etieskrill.engine.graphics.gl.shader.impl;

import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

public class PhongNoMaterialShader extends ShaderProgram {
    @Override
    protected String[] getShaderFileNames() {
        return new String[]{"PhongNoMaterial.glsl"};
    }

    @Override
    protected void getUniformLocations() {
        addUniform("model", Uniform.Type.MAT4);
        addUniform("combined", Uniform.Type.MAT4);
        addUniform("normal", Uniform.Type.MAT3);
        addUniform("lightCombined", Uniform.Type.MAT4);

        addUniform("viewPosition", Uniform.Type.VEC3);
        addUniform("blinnPhong", Uniform.Type.BOOLEAN);

        addUniformArray("globalLights", 1, Uniform.Type.STRUCT);
        addUniformArray("pointLights", 2, Uniform.Type.STRUCT);

        addUniform("shadowMap", Uniform.Type.SAMPLER2D); //TODO additional sampler types + uniform autodetect
        addUniform("pointShadowMaps", Uniform.Type.SAMPLER2D);
        addUniform("pointShadowFarPlane", Uniform.Type.FLOAT);
    }
}
