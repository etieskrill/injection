package org.etieskrill.engine.graphics.gl;

import glm.mat._4.Mat4;
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
    
    public void render(org.etieskrill.engine.graphics.assimp.Model model, ShaderProgram shader) {
        shader.setUniformMat4("uModel", model.getTransform());
        shader.setUniformMat3("uNormal", model.getTransform()
                .inverse_().transpose().toMat3_());
        //System.out.println("\033[0;32m" + matToString(model.getTransform()) + "\033[0m\n");
        for (int i = 0; i < model.getMeshes().size(); i++) {
            //System.out.println("mesh nr: " + i);
            render(model.getMeshes().get(i), shader);
        }
    }
    
    public void render(Mesh mesh, ShaderProgram shader) {
        bindMaterial(mesh, shader);
        shader.setUniformMat4("uMesh", mesh.getTransform());
        
        glBindVertexArray(mesh.getVao());
        
        shader.start();
        glDrawElements(GL_TRIANGLES, mesh.getIndices().size(), GL_UNSIGNED_SHORT, 0);
        shader.stop();
        
        glBindVertexArray(0);
    }
    
    private void bindMaterial(Mesh mesh, ShaderProgram shader) {
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
            
            int validTextures = diffuse + specular + emissive;
            texture.bind(validTextures);
            shader.setUniformInt_("material." + texture.getType().name().toLowerCase() + number, validTextures);
        }
        
        shader.setUniformFloat_("material.shininess", mesh.getMaterial().getShininess());
    }
    
    private String matToString(Mat4 mat) {
        return String.format("""
                        [%6.3f, %6.3f, %6.3f, %6.3f]
                        [%6.3f, %6.3f, %6.3f, %6.3f]
                        [%6.3f, %6.3f, %6.3f, %6.3f]
                        [%6.3f, %6.3f, %6.3f, %6.3f]""",
                mat.m00, mat.m01, mat.m02, mat.m03,
                mat.m10, mat.m11, mat.m12, mat.m13,
                mat.m20, mat.m21, mat.m22, mat.m23,
                mat.m30, mat.m31, mat.m32, mat.m33);
    }
    
}
