package org.etieskrill.engine.graphics.gl;

public class StaticShader extends ShaderProgram {
    
    private static final String VERTEX_FILE = "shaders/Fade.vert";
    private static final String FRAGMENT_FILE = "shaders/Fade.frag";
    
    public StaticShader() {
        super(VERTEX_FILE, FRAGMENT_FILE);
    }
    
    @Override
    protected void bindAttributes() {
        super.bindAttribute(0, "position");
    }
    
}
