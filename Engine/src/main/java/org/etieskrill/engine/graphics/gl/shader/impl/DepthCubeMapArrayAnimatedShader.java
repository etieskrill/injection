package org.etieskrill.engine.graphics.gl.shader.impl;

import org.etieskrill.engine.graphics.gl.shader.Shaders;

public class DepthCubeMapArrayAnimatedShader extends Shaders.DepthCubeMapArrayShader {

    @Override
    protected void init() {
        hasGeometryShader();
    }

    @Override
    protected String[] getShaderFileNames() {
        return new String[]{"DepthCubeMapArrayAnimated.glsl"};
    }

}
