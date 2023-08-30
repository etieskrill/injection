package org.etieskrill.engine.graphics.gl;

import glm_.mat4x4.Mat4;
import org.etieskrill.engine.graphics.assimp.Material;
import org.etieskrill.engine.graphics.assimp.Mesh;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Vector;

import static org.lwjgl.opengl.GL33C.*;

public class Renderer {
    
    private static final float clearColour = 0.25f;//0.025f;
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    public void prepare() {
        //logger.debug("New render cycle");
        glClearColor(clearColour, clearColour, clearColour, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    }
    
    public void render(Model model, ShaderProgram shader, Mat4 combined) {
        shader.setUniform("uCombined", combined);
        shader.setUniform("uModel", model.getTransform());
        shader.setUniform("uNormal", model.getTransform().inverse().transpose().toMat3());
    
        shader.start();
        for (Mesh mesh : model.getMeshes())
            render(mesh, shader);
        shader.stop();
    }
    
    private void render(Mesh mesh, ShaderProgram shader) {
        bindMaterial(mesh.getMaterial(), shader);
        shader.setUniform("uMesh", mesh.getTransform());
        glBindVertexArray(mesh.getVao());
        
        glDrawElements(GL_TRIANGLES, mesh.getNumIndices(), GL_UNSIGNED_SHORT, 0);
    
        for (int i = 0; i < 32; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    
        glBindVertexArray(0);
    }
    
    private void bindMaterial(Material material, ShaderProgram shader) {
        int diffuse = 0, specular = 0, emissive = 0, height = 0, shininess = 0;
        Vector<Texture> textures = material.getTextures();
    
        for (Texture texture : textures) {
            int number = switch (texture.getType()) {
                case DIFFUSE -> diffuse++;
                case SPECULAR -> specular++; //TODO could pass texture colour channels here, but it is probs best to just finally define a standard for this whole shebang
                case EMISSIVE -> emissive++;
                case HEIGHT -> height++;
                case SHININESS -> shininess++;
                case UNKNOWN -> throw new IllegalStateException("Texture has invalid type");
            };
        
            int validTextures = (diffuse + specular + emissive + height + shininess) - 1;
            texture.bind(validTextures);
//            if (mesh.getNumIndices() < 400) logger.trace("Binding texture of type {} as material.{}{} to unit {}",
//                    texture.getType().name(), texture.getType().name().toLowerCase(), number, validTextures);
            shader.setUniform("material." + texture.getType().name().toLowerCase() + number, validTextures);
        }
        
        //TODO either use non-register methods here, or more strongly hint at/enforce registration
        shader.setUniform("material.shininess", material.getShininess());
        shader.setUniform("material.specularity", material.getShininessStrength());
    }
    
}
