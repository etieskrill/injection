package org.etieskrill.engine.graphics.gl.shaders;

public class Shaders {

    public static ShaderProgram getStandardShader() {
        return new StaticShader();
    }

    public static ShaderProgram getRoundedBoxShader() {
        return new RoundedBoxShader();
    }

    public static ShaderProgram getLightSourceShader() {
        return new LightSourceShader();
    }
    
    public static ShaderProgram getTextureShader() {
        return new TextureShader();
    }

    private static class StaticShader extends ShaderProgram {
        private static final String VERTEX_FILE = "shaders/Phong.vert";
        private static final String FRAGMENT_FILE = "shaders/Phong.frag";

        public StaticShader() {
            super(VERTEX_FILE, FRAGMENT_FILE);
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
    
    //private static class ContainerShader extends ShaderProgram {
    //    private static final String VERETX_FILE
    //}

    private static class RoundedBoxShader extends ShaderProgram {
        private static final String VERTEX_FILE = "shaders/RoundedBox.vert";
        private static final String FRAGMENT_FILE = "shaders/RoundedBox.frag";

        public RoundedBoxShader() {
            super(VERTEX_FILE, FRAGMENT_FILE);
        }

        @Override
        protected void getUniformLocations() {}
    }

    private static class LightSourceShader extends ShaderProgram {
        private static final String VERTEX_FILE = "shaders/LightSource.vert";
        private static final String FRAGMENT_FILE = "shaders/LightSource.frag";

        public LightSourceShader() {
            super(VERTEX_FILE, FRAGMENT_FILE);
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
        private static final String VERTEX_FILE = "shaders/Texture.vert";
        private static final String FRAGMENT_FILE = "shaders/Texture.frag";
        
        public TextureShader() {
            super(VERTEX_FILE, FRAGMENT_FILE);
        }
        
        @Override
        protected void getUniformLocations() {
            addUniform("uModel");
            addUniform("uCombined");
            addUniform("diffuseMap");
            addUniform("uColour");
            //addUniform("uTime");
        }
    }

}
