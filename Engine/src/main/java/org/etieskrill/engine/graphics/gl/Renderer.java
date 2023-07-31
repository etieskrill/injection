package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.graphics.assimp.Mesh;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;

import java.util.Vector;

import static org.lwjgl.opengl.GL33C.*;

public class Renderer {
    
    private static final float clearColour = 0.5f;//0.025f;
    
    public void prepare() {
        glClearColor(clearColour, clearColour, clearColour, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }
    
    public void render(RawModel model) {
        glBindVertexArray(model.getVao());
        if (model instanceof Model texModel) texModel.bind();
        
        if (model.hasIndexBuffer())
            glDrawElements(model.getDrawMode(), model.getNumVertices(), GL_UNSIGNED_SHORT, 0);
        else
            glDrawArrays(model.getDrawMode(), 0, model.getNumVertices());
        
        if (model instanceof Model) Model.unbindTextures();
        glBindVertexArray(0);
    }
    
    public void render(Mesh mesh, ShaderProgram shader) {
        bindTextures(mesh, shader);
        glBindVertexArray(mesh.getVao());
        shader.start();
        glDrawElements(GL_TRIANGLES, mesh.getIndices().size(), GL_UNSIGNED_SHORT, 0);
        shader.stop();
        glBindVertexArray(0);
    }
    
    public void render(org.etieskrill.engine.graphics.assimp.Model model, ShaderProgram shader) {
        for (Mesh mesh : model.getMeshes()) {
            render(mesh, shader);
        }
    }
    
    private void bindTextures(Mesh mesh, ShaderProgram shader) {
        int diffuse = 0, specular = 0, emissive = 0;
        Vector<Texture> textures = mesh.getMaterial().getTextures();
        
        for (int i = 0; i < textures.size(); i++) {
            Texture texture = textures.get(i);
            int number = 0;
            switch (texture.getType()) {
                case DIFFUSE -> number = diffuse++;
                case SPECULAR -> number = specular++;
                case EMISSIVE -> number = emissive++;
                case UNKNOWN -> throw new IllegalStateException("Texture has invalid type");
            }
            
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE, texture.getTextureID());
            shader.setUniformInt("material." + texture.getType().name().toLowerCase() + number, i);
        }
    }
    
}
