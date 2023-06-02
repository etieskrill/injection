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
        private static final String VERTEX_FILE = "shaders/Fade.vert";
        private static final String FRAGMENT_FILE = "shaders/Fade.frag";

        public StaticShader() {
            super(VERTEX_FILE, FRAGMENT_FILE);
        }

        @Override
        protected void getUniformLocations() {
            addUniform("texture1");
            addUniform("texture2");
            addUniform("uModel");
            addUniform("uView");
            addUniform("uProjection");
            addUniform("uObjectColour");
            addUniform("uLightColour");
            addUniform("uAmbientStrength");
            addUniform("uLightPosition");
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
            addUniform("uLightColour");
        }
    }

}
