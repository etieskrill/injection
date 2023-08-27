package org.etieskrill.engine.graphics;

import glm_.mat4x4.Mat4;
import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.gl.*;
import org.etieskrill.engine.graphics.gl.shaders.ShaderFactory;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;

//TODO the Renderer currently does almost everything from here, this should only have batching mechanisms (duh)
@Deprecated
public class Batch implements Disposable {
    
    private final Renderer renderer;
    private final ModelFactory models;
    
    private ShaderProgram shader;
    
    private final Mat4 transform, combined;
    
    public Batch(Renderer renderer, ModelFactory models) {
        this.renderer = renderer;
        this.models = models;
        this.shader = ShaderFactory.getTextureShader();
        this.transform = new Mat4(1f);
        this.combined = new Mat4(1f);
    }
    
    public void render(Model model) {
        shader.start();
    
        //System.out.println(Arrays.toString(transform.toFa_()) + "\n" + Arrays.toString(combined.toFa_()));

        shader.setUniformMat4("uModel", transform);
        shader.setUniformMat4("uCombined", combined);
    
        //System.out.println(Arrays.toString(transform.toFa_()) + "\n" + Arrays.toString(combined.toFa_()) + "\n\n");
        
        //renderer.render(model);
        
        shader.stop();
    }
    
    public ModelFactory getModelFactory() {
        return models;
    }
    
    public void setTransform(Mat4 mat) {
        this.transform.put(mat);
    }
    
    public void resetTransform() {
        this.transform.put(1f);
    }
    
    public void setCombined(Mat4 mat) {
        this.combined.put(mat);
    }
    
    public ShaderProgram getShader() {
        return shader;
    }
    
    public void setShader(ShaderProgram shader) {
        this.shader = shader;
    }
    
    @Override
    public void dispose() {
        shader.dispose();
    }
    
}
