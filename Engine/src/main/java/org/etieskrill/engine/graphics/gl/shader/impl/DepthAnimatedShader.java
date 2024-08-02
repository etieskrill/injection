package org.etieskrill.engine.graphics.gl.shader.impl;

import org.etieskrill.engine.graphics.gl.shader.Shaders;

public class DepthAnimatedShader extends Shaders.DepthShader {
    @Override
    protected String[] getShaderFileNames() {
        return new String[]{"DepthAnimated.glsl"};
    }
}
