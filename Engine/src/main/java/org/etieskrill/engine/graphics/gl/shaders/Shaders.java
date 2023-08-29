package org.etieskrill.engine.graphics.gl.shaders;

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

    private static class StaticShader extends ShaderProgram {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"Phong.vert", "Phong.frag"};
        }
        
        @Override
        protected void getUniformLocations() {
            addUniform("uMesh");
            addUniform("uModel");
            addUniform("uNormal");
            addUniform("uCombined");
            
            addUniform("uViewDirection");
            
            addUniform("material.diffuse0");
            addUniform("material.specular0");
            addUniform("material.emissive0");
            addUniform("material.shininess");
            addUniform("material.specularity");
        }
        
    }
    
    private static class ContainerShader extends StaticShader {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"Container.vert", "Container.frag"};
        }
    
        @Override
        protected void getUniformLocations() {
            super.getUniformLocations();
            addUniform("uTime");
        }
    }
    
    private static class SwordShader extends StaticShader {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"Sword.vert", "Sword.frag"};
        }
    
        @Override
        protected void getUniformLocations() {
            super.getUniformLocations();
            addUniform("uTime");
        }
    }

    private static class RoundedBoxShader extends ShaderProgram {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"RoundedBox.vert", "RoundedBox.frag"};
        }
        
        @Override
        protected void getUniformLocations() {}
    }

    private static class LightSourceShader extends ShaderProgram {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"LightSource.vert", "LightSource.frag"};
        }
        
        @Override
        protected void getUniformLocations() {
            addUniform("uMesh");
            addUniform("uModel");
            addUniform("uCombined");
            addUniform("light.ambient");
            addUniform("light.diffuse");
            addUniform("light.specular");
        }
    }
    
    private static class TextureShader extends ShaderProgram {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"Texture.vert", "Texture.frag"};
        }
        
        @Override
        protected void getUniformLocations() {
            addUniform("uModel");
            addUniform("uCombined");
            addUniform("diffuseMap");
            addUniform("uColour");
        }
    }
    
    private static class SingleColourShader extends ShaderProgram {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"SingleColour.vert", "SingleColour.frag"};
        }
    
        @Override
        protected void getUniformLocations() {
            //TODO i've forgotten about this thing waaaay to often, either enforce via enums soon or rework this goddamned system
            addUniform("uMesh");
            addUniform("uModel");
            addUniform("uNormal");
            addUniform("uCombined");
            
            addUniform("uColour");
        }
    }

}
