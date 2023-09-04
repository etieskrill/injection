package org.etieskrill.engine.graphics.gl.shaders;

import static org.etieskrill.engine.graphics.gl.shaders.ShaderProgram.Uniform.Type.*;

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
    
    public static PostprocessingShader getPostprocessingShader() {
        return new PostprocessingShader();
    }

    private static class StaticShader extends ShaderProgram {
        @Override
        protected void init() {}
    
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"Phong.vert", "Phong.frag"};
        }
        
        @Override
        protected void getUniformLocations() {
            //TODO theoretically, a sort of autodetect feature is entirely possible
            addUniform("uModel", MAT4);
            addUniform("uNormal", MAT3);
            addUniform("uCombined", MAT4);
            
            addUniform("uViewPosition", VEC3);
            
            addUniform("material.diffuse0", SAMPLER2D);
            addUniform("material.specular0", SAMPLER2D);
            addUniform("material.emissive0", SAMPLER2D);
            addUniform("material.shininess", FLOAT);
            addUniform("material.specularity", FLOAT);
            
            addUniformArray("globalLights[$].direction", 1, VEC3);
            addUniformArray("globalLights[$].ambient", 1, VEC3);
            addUniformArray("globalLights[$].diffuse", 1, VEC3);
            addUniformArray("globalLights[$].specular", 1, VEC3);
    
            addUniformArray("lights[$].position", 2, VEC3);
            addUniformArray("lights[$].ambient", 2, VEC3);
            addUniformArray("lights[$].diffuse", 2, VEC3);
            addUniformArray("lights[$].specular", 2, VEC3);
            addUniformArray("lights[$].constant", 2, FLOAT);
            addUniformArray("lights[$].linear", 2, FLOAT);
            addUniformArray("lights[$].quadratic", 2, FLOAT);
        }
        
    }
    
    private static class ContainerShader extends StaticShader {
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
    
    private static class SwordShader extends StaticShader {
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

    private static class RoundedBoxShader extends ShaderProgram {
        @Override
        protected void init() {}
    
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"RoundedBox.vert", "RoundedBox.frag"};
        }
        
        @Override
        protected void getUniformLocations() {}
    }

    private static class LightSourceShader extends ShaderProgram {
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
            addUniform("uModel", MAT4);
            addUniform("uCombined", MAT4);
            addUniform("light.ambient", FLOAT);
            addUniform("light.diffuse", FLOAT);
            addUniform("light.specular", FLOAT);
        }
    }
    
    private static class TextureShader extends ShaderProgram {
        @Override
        protected void init() {}
        
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"Texture.vert", "Texture.frag"};
        }
        
        @Override
        protected void getUniformLocations() {
            addUniform("uModel", MAT4);
            addUniform("uCombined", MAT4);
            addUniform("diffuseMap", SAMPLER2D);
            addUniform("uColour", VEC3);
        }
    }
    
    private static class SingleColourShader extends ShaderProgram {
        @Override
        protected void init() {}
        
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"SingleColour.vert", "SingleColour.frag"};
        }
    
        @Override
        protected void getUniformLocations() {
            //TODO i've forgotten about this thing waaaay to often, either enforce via enums soon or rework this goddamned system
            addUniform("uModel", MAT4);
            addUniform("uNormal", MAT3);
            addUniform("uCombined", MAT4);
            
            addUniform("uColour", VEC4);
        }
    }
    
    private static class OutlineShader extends ShaderProgram {
        @Override
        protected void init() {}
    
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"Outline.vert", "Outline.frag"};
        }
    
        @Override
        protected void getUniformLocations() {
            addUniform("uModel", MAT4);
            addUniform("uNormal", MAT3);
            addUniform("uCombined", MAT4);
            
            addUniform("uThicknessFactor", FLOAT);
            
            addUniform("uColour", VEC4);
        }
    }
    
    private static class PhongShininessMapShader extends StaticShader {
        @Override
        protected void init() {
            disableStrictUniformChecking();
        }
    
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"PhongShininessMap.vert", "PhongShininessMap.frag"};
        }
    
        @Override
        protected void getUniformLocations() {
            super.getUniformLocations();
            
            addUniform("material.shininess0", SAMPLER2D);
        }
    }
    
    private static class ScreenQuadShader extends ShaderProgram {
        @Override
        protected void init() {}
    
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"ScreenQuad.vert", "ScreenQuad.frag"};
        }
    
        @Override
        protected void getUniformLocations() {}
    }
    
    private static class PostprocessingShader extends ShaderProgram {
        @Override
        protected void init() {}
        
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"Postprocessing.vert", "Postprocessing.frag"};
        }
        
        @Override
        protected void getUniformLocations() {
            addUniform("uInvert", BOOLEAN);
            addUniform("uGrayscale", BOOLEAN);
            addUniform("uSharpen", BOOLEAN);
            addUniform("uSharpenOffset", FLOAT);
            addUniform("uBlur", BOOLEAN);
            addUniform("uBlurOffset", FLOAT);
            addUniform("uEdgeDetection", BOOLEAN);
            addUniform("uEmboss", BOOLEAN);
            addUniform("uEmbossOffset", FLOAT);
        }
    }

}
