package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.graphics.assimp.Mesh;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Vector;

import static org.lwjgl.opengl.GL33C.*;

public class Renderer {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private static final float clearColour = 0.25f;//0.025f;
    
    public void prepare() {
        //logger.debug("New render cycle");
        glClearColor(clearColour, clearColour, clearColour, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }
    
    public void render(Model model, ShaderProgram shader) {
        shader.setUniformMat4("uModel", model.getTransform());
        shader.setUniformMat3("uNormal", model.getTransform().inverse().transpose().toMat3());
        for (Mesh mesh : model.getMeshes())
            render(mesh, shader);
    }
    
    public void render(Mesh mesh, ShaderProgram shader) {
        bindMaterial(mesh, shader);
        shader.setUniformMat4("uMesh", mesh.getTransform());
        
        glBindVertexArray(mesh.getVao());
        shader.start();
        glDrawElements(GL_TRIANGLES, mesh.getNumIndices(), GL_UNSIGNED_SHORT, 0);
        shader.stop();
        
        glBindVertexArray(0);
    }
    
    private void bindMaterial(Mesh mesh, ShaderProgram shader) {
        int diffuse = 0, specular = 0, emissive = 0;
        Vector<Texture> textures = mesh.getMaterial().getTextures();
    
        for (Texture texture : textures) {
            int number = 0;
            switch (texture.getType()) {
                case DIFFUSE -> number = diffuse++;
                case SPECULAR -> number = specular++;
                case EMISSIVE -> number = emissive++;
                case UNKNOWN -> throw new IllegalStateException("Texture has invalid type");
            }
        
            int validTextures = (diffuse + specular + emissive) - 1;
            texture.bind(validTextures);
            //System.out.printf("Binding texture of type %s as material.%s%d to unit %d\n", texture.getType().name(), texture.getType().name().toLowerCase(), number, validTextures);
            shader.setUniformInt("material." + texture.getType().name().toLowerCase() + number, validTextures);
        }
        
        shader.setUniformFloat("material.shininess", 32f);
    }
    
}
