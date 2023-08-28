package org.etieskrill.engine.graphics.gl.shaders;

public class Shaders {

    public static StaticShader getStandardShader() {
        return new StaticShader();
    }
    
    public static ContainerShader getContainerShader() {
        return new ContainerShader();
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
            addUniform("uTime");
            
            addUniform("material.diffuse0");
            addUniform("material.specular0");
            addUniform("material.emissive0");
            addUniform("material.shininess");
        }
        
    }
    
    private static class ContainerShader extends StaticShader {
        @Override
        protected String[] getShaderFileNames() {
            return new String[]{"Container.vert", "Container.frag"};
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

}
