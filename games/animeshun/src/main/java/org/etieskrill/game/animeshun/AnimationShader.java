package org.etieskrill.game.animeshun;

import org.etieskrill.engine.graphics.data.DirectionalLight;
import org.etieskrill.engine.graphics.gl.shader.Shaders;
import org.joml.Matrix4fc;

import java.util.List;

import static org.etieskrill.engine.graphics.gl.shader.ShaderProgram.Uniform.Type.*;

public class AnimationShader extends Shaders.StaticShader {

    private static final int MAX_BONES = 100;

    @Override
    protected void init() {
        disableStrictUniformChecking();
    }

    @Override
    protected String[] getShaderFileNames() {
        return new String[]{"Animation.vert", "Animation.frag"};
    }

    @Override
    protected void getUniformLocations() {
        super.getUniformLocations();

        addUniformArray("boneMatrices", MAX_BONES, MAT4);

        addUniformArray("globalLights", 1, STRUCT);
        addUniformArray("lights", 2, STRUCT);

        addUniform("uShowBoneSelector", INT);
        addUniform("uShowBoneWeights", BOOLEAN);
    }

    public void setBoneMatrices(List<Matrix4fc> boneMatrices) {
        setUniformArray("boneMatrices", boneMatrices.toArray());
    }

    public void setShowBoneSelector(int boneGroup) {
        if (boneGroup < 0 || boneGroup > 4)
            throw new IllegalArgumentException("Selected bone group must be between 0 and 4");
        setUniform("uShowBoneSelector", boneGroup);
    }

    public void setShowBoneWeights(boolean showBoneWeights) {
        setUniform("uShowBoneWeights", showBoneWeights);
    }

    public void setGlobalLight(DirectionalLight light) {
        setUniformArray("globalLights", 0, light);
    }

}
