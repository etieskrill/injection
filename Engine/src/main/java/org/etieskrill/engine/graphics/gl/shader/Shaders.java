package org.etieskrill.engine.graphics.gl.shader;

import org.etieskrill.engine.graphics.data.DirectionalLight;
import org.etieskrill.engine.graphics.data.PointLight;
import org.etieskrill.engine.graphics.texture.CubeMapTexture;
import org.joml.*;

import static org.etieskrill.engine.graphics.gl.shader.ShaderProgram.Uniform.Type.*;

public class Shaders {

    public static StaticShader getStandardShader() {
        return new StaticShader();
    }

    public static ContainerShader getContainerShader() {
        return new ContainerShader();
    }

    public static SwordShader getSwordShader() {
        return new SwordShader();
    }

    public static RoundedBoxShader getRoundedBoxShader() {
        return new RoundedBoxShader();
    }

    public static LightSourceShader getLightSourceShader() {
        return new LightSourceShader();
    }

    public static TextureShader getTextureShader() {
        return new TextureShader();
    }

    public static SingleColourShader getSingleColourShader() {
        return new SingleColourShader();
    }

    public static OutlineShader getOutlineShader() {
        return new OutlineShader();
    }

    public static PhongShininessMapShader getBackpackShader() {
        return new PhongShininessMapShader();
    }

    public static ScreenQuadShader getScreenShader() {
        return new ScreenQuadShader();
    }

    public static CubeMapShader getCubeMapShader() {
        return new CubeMapShader();
    }

    public static PostprocessingShader getPostprocessingShader() {
        return new PostprocessingShader();
    }

    public static TextShader getTextShader() {
        return new TextShader();
    }

    //TODO evaluate whether to put uniforms as variables
    public static class StaticShader extends ShaderProgram {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"Phong.vert", "Phong.frag"};
        }

        @Override
        protected void getUniformLocations() {
            //TODO theoretically, a sort of autodetect feature is entirely possible
            addUniform("uViewPosition", VEC3);
            addUniform("uTextureScale", VEC2, new Vector2f(1f));

            addUniform("material.specularTexture", BOOLEAN);
            addUniform("uNormalMapped", BOOLEAN);
            addUniform("uBlinnPhong", BOOLEAN, true);

            addUniformArray("globalLights", 1, STRUCT);
            addUniformArray("lights", 2, STRUCT);

//            addUniform("globalShadowMap", SAMPLER2D);
            addUniform("pointShadowMaps", SAMPLER_CUBE_MAP_ARRAY);

            addUniform("pointShadowFarPlane", FLOAT, 20f);
        }

        public void setTextureScale(Vector2fc textureScale) {
            setUniform("uTextureScale", textureScale);
        }

        public void setNormalMapped(boolean normalMapped) {
            setUniform("uNormalMapped", normalMapped);
        }

        public void setBlinnPhong(boolean blinnPhong) {
            setUniform("uBlinnPhong", blinnPhong);
        }

        public void setViewPosition(Vector3fc viewPosition) {
            setUniform("uViewPosition", viewPosition);
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

    public static class ContainerShader extends StaticShader {
        @Override
        protected void init() {
            disableStrictUniformChecking();
        }

        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"Container.vert", "Container.frag"};
        }

        @Override
        protected void getUniformLocations() {
            super.getUniformLocations();
            addUniform("uTime", FLOAT);
        }
    }

    public static class SwordShader extends StaticShader {
        @Override
        protected void init() {
            disableStrictUniformChecking();
        }

        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"Sword.vert", "Sword.frag"};
        }

        @Override
        protected void getUniformLocations() {
            super.getUniformLocations();
            addUniform("uTime", FLOAT);
        }
    }

    public static class RoundedBoxShader extends ShaderProgram {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"RoundedBox.vert", "RoundedBox.frag"};
        }

        @Override
        protected void getUniformLocations() {
        }
    }

    public static class LightSourceShader extends ShaderProgram {
        @Override
        protected void init() {
            disableStrictUniformChecking();
        }

        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"LightSource.vert", "LightSource.frag"};
        }

        @Override
        protected void getUniformLocations() {
            addUniform("light", STRUCT);
        }

        public void setLight(PointLight light) {
            setUniform("light", light);
        }

        public void setLight(DirectionalLight light) {
            setUniform("light", light);
        }
    }

    public static class TextureShader extends ShaderProgram {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"Texture.vert", "Texture.frag"};
        }

        @Override
        protected void getUniformLocations() {
            addUniform("uColour", VEC4);
        }
    }

    public static class SingleColourShader extends ShaderProgram {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"SingleColour.vert", "SingleColour.frag"};
        }

        @Override
        protected void getUniformLocations() {
            //TODO i've forgotten about this thing waaaay to often, either enforce via enums soon or rework this goddamned system
            addUniform("uColour", VEC4);
        }
    }

    public static class OutlineShader extends ShaderProgram {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"Outline.vert", "Outline.frag"};
        }

        @Override
        protected void getUniformLocations() {
            addUniform("uThicknessFactor", FLOAT);

            addUniform("uColour", VEC4);
        }
    }

    public static class PhongShininessMapShader extends StaticShader {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"PhongShininessMap.vert", "PhongShininessMap.frag"};
        }

        @Override
        protected void getUniformLocations() {
            super.getUniformLocations();
        }
    }

    public static class ScreenQuadShader extends ShaderProgram {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"ScreenQuad.vert", "ScreenQuad.frag"};
        }

        @Override
        protected void getUniformLocations() {
        }
    }

    public static class PostprocessingShader extends ShaderProgram {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"Postprocessing.vert", "Postprocessing.frag"};
        }

        @Override
        protected void getUniformLocations() {
            addUniform("uInvert", BOOLEAN);
            addUniform("uColour", VEC3);
            addUniform("uGrayscale", BOOLEAN);
            addUniform("uSharpen", BOOLEAN);
            addUniform("uSharpenOffset", FLOAT);
            addUniform("uBlur", BOOLEAN);
            addUniform("uBlurOffset", FLOAT);
            addUniform("uEdgeDetection", BOOLEAN);
            addUniform("uEmboss", BOOLEAN);
            addUniform("uEmbossOffset", FLOAT);
            addUniform("uGammaCorrection", BOOLEAN);
            addUniform("uGammaFactor", FLOAT);
        }

        public void doInvert(boolean invert) {
            setUniform("uInvert", invert);
        }

        public void setColour(Vector3f colour) {
            setUniform("uColour", colour);
        }

        public void doGrayscale(boolean grayscale) {
            setUniform("uGrayscale", grayscale);
        }

        public void doSharpen(boolean sharpen) {
            setUniform("uSharpen", sharpen);
        }

        public void setSharpenOffset(float sharpenOffset) {
            setUniform("uSharpenOffset", sharpenOffset);
        }

        public void doBlur(boolean blur) {
            setUniform("uBlur", blur);
        }

        public void setBlurOffset(float blurOffset) {
            setUniform("uBlurOffset", blurOffset);
        }

        public void doEdgeDetection(boolean edgeDetection) {
            setUniform("uEdgeDetection", edgeDetection);
        }

        public void doEmboss(boolean emboss) {
            setUniform("uEmboss", emboss);
        }

        public void setEmbossOffset(float embossOffset) {
            setUniform("uEmbossOffset", embossOffset);
        }

        public void doGammaCorrection(boolean gammaCorrection) {
            setUniform("uGammaCorrection", gammaCorrection);
        }

        public void setGammaFactor(float gammaFactor) {
            setUniform("uGammaFactor", gammaFactor);
        }
    }

    public static class CubeMapShader extends ShaderProgram {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"CubeMap.vert", "CubeMap.frag"};
        }

        @Override
        protected void getUniformLocations() {
        }
    }

    public static class TextShader extends ShaderProgram {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{
                    "Text.vert",
                    "Text.geom",
                    "Text.frag"};
        }

        @Override
        protected void getUniformLocations() {
        }
    }

    public static class ShowNormalsShader extends ShaderProgram {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{
                    "ShowNormals.vert",
                    "ShowNormals.geom",
                    "ShowNormals.frag"
            };
        }

        @Override
        protected void getUniformLocations() {
        }
    }

    public static class DepthShader extends ShaderProgram {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{
                    "Depth.vert",
                    "Depth.frag"
            };
        }

        @Override
        protected void getUniformLocations() {
        }
    }

    public static class DepthCubeMapShader extends ShaderProgram {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{
                    "DepthCubeMap.vert",
                    "DepthCubeMap.geom",
                    "DepthCubeMap.frag"
            };
        }

        @Override
        protected void getUniformLocations() {
            addUniformArray("shadowCombined", CubeMapTexture.NUM_SIDES, MAT4);
            addUniform("light", STRUCT);
            addUniform("farPlane", FLOAT);
        }

        public void setShadowCombined(Matrix4fc[] shadowCombined) {
            if (shadowCombined.length != CubeMapTexture.NUM_SIDES)
                throw new IllegalArgumentException("Shadow map combined matrices must contain 6 matrices, but was " + shadowCombined.length);
            setUniformArray("shadowCombined", shadowCombined);
        }

        public void setLight(PointLight light) {
            setUniform("light", light);
        }

        public void setFarPlane(float farPlane) {
            setUniform("farPlane", farPlane);
        }
    }

    public static class DepthCubeMapArrayShader extends ShaderProgram {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{
                    "DepthCubeMapArray.vert",
                    "DepthCubeMapArray.geom",
                    "DepthCubeMapArray.frag"
            };
        }

        @Override
        protected void getUniformLocations() {
            addUniformArray("shadowCombined", CubeMapTexture.NUM_SIDES, MAT4);
            addUniform("light", STRUCT);
            addUniform("farPlane", FLOAT);
            addUniform("index", INT);
        }

        public void setShadowCombined(Matrix4fc[] shadowCombined) {
            if (shadowCombined.length != CubeMapTexture.NUM_SIDES)
                throw new IllegalArgumentException("Shadow map combined matrices must contain 6 matrices, but was " + shadowCombined.length);
            setUniformArray("shadowCombined", shadowCombined);
        }

        public void setLight(PointLight light) {
            setUniform("light", light);
        }

        public void setFarPlane(float farPlane) {
            setUniform("farPlane", farPlane);
        }

        public void setIndex(int index) {
            setUniform("index", index);
        }
    }

}
