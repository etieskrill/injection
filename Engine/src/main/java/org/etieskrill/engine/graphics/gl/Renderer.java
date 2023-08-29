package org.etieskrill.engine.graphics.gl;

import glm_.mat4x4.Mat4;
import glm_.vec3.Vec3;
import glm_.vec4.Vec4;
import org.etieskrill.engine.graphics.Camera;
import org.etieskrill.engine.graphics.assimp.Material;
import org.etieskrill.engine.graphics.assimp.Mesh;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Vector;

import static org.lwjgl.opengl.GL33C.*;

public class Renderer {
    
    private static final float clearColour = 0.25f;//0.025f;
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final ShaderProgram singleColourShader = Shaders.getSingleColourShader();
    
    public void prepare() {
        //logger.debug("New render cycle");
        glClearColor(clearColour, clearColour, clearColour, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    }
    
    public void render(Model model, ShaderProgram shader, Mat4 combined) {
        shader.setUniformMat4("uCombined", combined);
        shader.setUniformMat4("uModel", model.getTransform());
        shader.setUniformMat3("uNormal", model.getTransform().inverse().transpose().toMat3());
    
        shader.start();
        for (Mesh mesh : model.getMeshes())
            render(mesh, shader);
        shader.stop();
    }
    
    private void render(Mesh mesh, ShaderProgram shader) {
        bindMaterial(mesh.getMaterial(), shader);
        shader.setUniformMat4("uMesh", mesh.getTransform());
        glBindVertexArray(mesh.getVao());
        
        glDrawElements(GL_TRIANGLES, mesh.getNumIndices(), GL_UNSIGNED_SHORT, 0);
    
        for (int i = 0; i < 32; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    
        glBindVertexArray(0);
    }
    
    public void renderSimpleHighlight(Model model, ShaderProgram shader, Mat4 combined, Vec3 highlightScale, Vec3 highlightOffset) {
        shader.setUniformMat4("uCombined", combined);
        shader.setUniformMat4("uModel", model.getTransform());
        shader.setUniformMat3("uNormal", model.getTransform().inverse().transpose().toMat3());
        
        singleColourShader.setUniformMat4("uCombined", combined);
        singleColourShader.setUniformMat4("uModel", model.getTransform());
        singleColourShader.setUniformMat3("uNormal", model.getTransform().inverse().transpose().toMat3());
        
        shader.start();
        for (Mesh mesh : model.getMeshes())
            renderSimpleHighlight(model, mesh, shader, highlightScale, highlightOffset);
        shader.stop();
    }

    private void renderSimpleHighlight(Model model, Mesh mesh, ShaderProgram shader, Vec3 highlightScale, Vec3 highlightOffset) {
        shader.start();
        bindMaterial(mesh.getMaterial(), shader);
        singleColourShader.setUniformMat4("uMesh", mesh.getTransform());
        shader.setUniformMat4("uMesh", mesh.getTransform());
        glBindVertexArray(mesh.getVao());

        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);

        glStencilFunc(GL_ALWAYS, 1, 0xFF);
        glStencilMask(0xFF);
        glDrawElements(GL_TRIANGLES, mesh.getNumIndices(), GL_UNSIGNED_SHORT, 0);

        glStencilFunc(GL_NOTEQUAL, 1, 0xFF);
        glStencilMask(0x00);
        glDisable(GL_DEPTH_TEST);

        Vec3 scale = new Vec3(model.getScale());
        Vec3 pos = new Vec3(model.getPosition());
        model.getScale().timesAssign(1.05f);
        if (model.getName().equals("Sting-Sword")) {
            model.getPosition().plusAssign(0f, 0.1f, 0f);
        }
        shader.setUniformMat4("uModel", model.getTransform());
        shader.setUniformMat3("uNormal", model.getTransform().inverse().transpose().toMat3());
        singleColourShader.setUniformMat4("uModel", model.getTransform());
        singleColourShader.setUniformMat3("uNormal", model.getTransform().inverse().transpose().toMat3());
        singleColourShader.setUniformMat4("uMesh", mesh.getTransform());

        singleColourShader.start();
        singleColourShader.setUniformVec4("uColour", new Vec4(1f, 0f, 0f, 0.5f));

        glDrawElements(GL_TRIANGLES, mesh.getNumIndices(), GL_UNSIGNED_SHORT, 0);

        model.setScale(scale);
        model.setPosition(pos);

        glStencilMask(0xFF);
        glStencilFunc(GL_ALWAYS, 1, 0xFF);
        glEnable(GL_DEPTH_TEST);

        //TODO instead of implementing this, just read up on such attributes across gl versions and different systems
        // and define an internal spec to choose fitting constants
        //glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS);
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
            shader.setUniformInt("material." + texture.getType().name().toLowerCase() + number, validTextures);
        }
        
        //TODO either use non-register methods here, or more strongly hint at/enforce registration
        shader.setUniformFloat("material.shininess", material.getShininess());
        shader.setUniformFloat("material.specularity", material.getShininessStrength());
    }
    
}
