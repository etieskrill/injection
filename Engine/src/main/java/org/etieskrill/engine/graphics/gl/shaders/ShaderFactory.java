package org.etieskrill.engine.graphics.gl.shaders;

public class ShaderFactory {

    public static ShaderProgram getStandardShader() {
        return new StaticShader();
    }

    public static ShaderProgram getRoundedBoxShader() {
        return new RoundedBoxShader();
    }

    static class StaticShader extends ShaderProgram {
        private static final String VERTEX_FILE = "shaders/Fade.vert";
        private static final String FRAGMENT_FILE = "shaders/Fade.frag";

        public StaticShader() {
            super(VERTEX_FILE, FRAGMENT_FILE);
        }

        @Override
        protected void bindAttributes() {
            //TODO perhaps this is a duplicate of the layout(... = 0) specifier in the vertex shader?
            super.bindAttribute(0, "position");
        }
    }

    static class RoundedBoxShader extends ShaderProgram {
        private static final String VERTEX_FILE = "shaders/RoundedBox.vert";
        private static final String FRAGMENT_FILE = "shaders/RoundedBox.frag";

        public RoundedBoxShader() {
            super(VERTEX_FILE, FRAGMENT_FILE);
        }

        @Override
        protected void bindAttributes() {
            super.bindAttribute(0, "position");
        }
    }

}
