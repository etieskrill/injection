package org.etieskrill.engine.graphics.gl.shaders;

public class ShaderFactory {

    public static ShaderProgram getStandardShader() {
        return new StaticShader();
    }

    public static ShaderProgram getRoundedBoxShader() {
        return new RoundedBoxShader();
    }

    public static ShaderProgram getLightSourceShader() {
        return new LightSourceShader();
    }

    private static class StaticShader extends ShaderProgram {
        private static final String VERTEX_FILE = "shaders/Phong.vert";
        private static final String FRAGMENT_FILE = "shaders/Phong.frag";

        public StaticShader() {
            super(VERTEX_FILE, FRAGMENT_FILE);
        }

        @Override
        protected void getUniformLocations() {
            //samplers are not directly bound to a shader, but instead via the texture unit binding points
            //(something to do with samplers being of an opaque type?)
            //addUniform("diffuseMap");
            //addUniform("specularMap");
            
            addUniform("uModel");
            addUniform("uNormal");
            addUniform("uView");
            addUniform("uProjection");
            
            addUniform("uViewPosition");
            addUniform("uTime");
            
            addUniform("light.position");
            addUniform("light.ambient");
            addUniform("light.diffuse");
            addUniform("light.specular");
            
            addUniform("light.constant");
            addUniform("light.linear");
            addUniform("light.quadratic");
    
            addUniform("flashlight.position");
            addUniform("flashlight.direction");
            addUniform("flashlight.cutoff");
            
            addUniform("flashlight.ambient");
            addUniform("flashlight.diffuse");
            addUniform("flashlight.specular");
            
            addUniform("flashlight.constant");
            addUniform("flashlight.linear");
            addUniform("flashlight.quadratic");
            
            addUniform("material.diffuse");
            addUniform("material.specular");
            addUniform("material.emission");
            addUniform("material.shininess");
        }
        
    }

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
            addUniform("uModel");
            addUniform("uCombined");
            addUniform("light.ambient");
            addUniform("light.diffuse");
            addUniform("light.specular");
        }
    }

}
