package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.graphics.assimp.Material;
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
    
    private void render(Mesh mesh, ShaderProgram shader) {
        shader.start();
        bindMaterial(mesh.getMaterial(), shader);
        shader.setUniformMat4("uMesh", mesh.getTransform());
        glBindVertexArray(mesh.getVao());
        
        glDrawElements(GL_TRIANGLES, mesh.getNumIndices(), GL_UNSIGNED_SHORT, 0);
    
        for (int i = 0; i < 32; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    
        glBindVertexArray(0);
        shader.stop();
    }
    
    private void bindMaterial(Material material, ShaderProgram shader) {
        int diffuse = 0, specular = 0, emissive = 0, height = 0, shininess = 0;
        Vector<Texture> textures = material.getTextures();
    
        for (Texture texture : textures) {
            int number = switch (texture.getType()) {
                case DIFFUSE -> diffuse++;
                case SPECULAR -> specular++;
                case EMISSIVE -> emissive++;
                case HEIGHT -> height++;
                case SHININESS -> shininess++;
                case UNKNOWN -> throw new IllegalStateException("Texture has invalid type");
            };
        
            int validTextures = (diffuse + specular + emissive + height + shininess) - 1;
            texture.bind(validTextures);
//            if (mesh.getNumIndices() < 400) logger.trace("Binding texture of type {} as material.{}{} to unit {}",
//                    texture.getType().name(), texture.getType().name().toLowerCase(), number, validTextures);
            shader.setUniformInt("material." + texture.getType().name().toLowerCase() + number, validTextures);
        }
        
        shader.setUniformFloat("material.shininess", material.getShininess());
        shader.setUniformFloat("material.specularity", material.getShininessStrength());
    }
    
}
