package org.etieskrill.engine.graphics.gl;

import glm_.mat4x4.Mat4;
import glm_.vec4.Vec4;
import org.etieskrill.engine.graphics.assimp.CubeMapModel;
import org.etieskrill.engine.graphics.assimp.Material;
import org.etieskrill.engine.graphics.assimp.Mesh;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
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
        if (shader.isPlaceholder()) { //TODO qol feature, should have no performance implications
            renderOutline(model, shader, combined, 1f, new Vec4(1, 0, 1, 1), true);
            return;
        }
        
        _render(model, shader, combined);
    }
    
    //TODO update spec: all factory methods use loaders by default, constructors/builders do not
    private final ShaderProgram outlineShader = Shaders.getOutlineShader();
    
    //TODO add outline & wireframe as flag in render
    public void renderOutline(Model model, ShaderProgram shader, Mat4 combined) {
        renderOutline(model, shader, combined, 0.5f, new Vec4(1f, 0f, 0f, 1f), false);
    }
    
    public void renderOutline(Model model, ShaderProgram shader, Mat4 combined, float thickness, Vec4 colour, boolean writeToFront) {
        outlineShader.setUniform("uThicknessFactor", thickness);
        outlineShader.setUniform("uColour", colour);
        
        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
    
        glStencilFunc(GL_ALWAYS, 1, 0xFF);
        glStencilMask(0xFF);
        _render(model, shader, combined);
    
        glStencilFunc(GL_NOTEQUAL, 1, 0xFF);
        glStencilMask(0x00);
        if (writeToFront) glDisable(GL_DEPTH_TEST);
        _render(model, outlineShader, combined);
    
        glStencilFunc(GL_ALWAYS, 0, 0xFF);
        glStencilMask(0xFF);
        if (writeToFront) glEnable(GL_DEPTH_TEST);
    }
    
    public void renderWireframe(Model model, ShaderProgram shader, Mat4 combined) {
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        _render(model, shader, combined);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    }
    
    public void render(CubeMapModel cubemap, ShaderProgram shader, Mat4 combined) {
        //TODO perf increase use early depth test to discard instead of this
        glDepthMask(false);
        _render(cubemap, shader, combined);
        glDepthMask(true);
    }
    
    private void _render(Model model, ShaderProgram shader, Mat4 combined) {
        shader.setUniform("uCombined", combined, false);
        shader.setUniform("uModel", model.getTransform(), false);
        shader.setUniform("uNormal", model.getTransform().inverse().transpose().toMat3(), false);
    
        if (!model.doCulling()) glDisable(GL_CULL_FACE);
        if (model.hasTransparency()) glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        shader.start();
        for (Mesh mesh : model.getMeshes())
            render(mesh, shader);
        shader.stop();
        if (!model.doCulling()) glEnable(GL_CULL_FACE);
        if (model.hasTransparency()) glBlendFunc(GL_ONE, GL_ZERO);
    }
    
    //TODO should once again be made private, so dont use anywhere else, even if you **could**
    @Deprecated
    public void render(Mesh mesh, ShaderProgram shader) {
        bindMaterial(mesh.getMaterial(), shader);
        glBindVertexArray(mesh.getVao());
        
        glDrawElements(GL_TRIANGLES, mesh.getNumIndices(), GL_UNSIGNED_SHORT, 0);
    
        Texture.unbindAllTextureUnits();
    
        glBindVertexArray(0);
    }
    
    private void bindMaterial(Material material, ShaderProgram shader) {
        //TODO here the renderer could decide what kind of shader to use, based off of the material given
        int tex2d = 0, cubemaps = 0;
        int diffuse = 0, specular = 0, emissive = 0, height = 0, shininess = 0;
        Vector<Texture> textures = material.getTextures();
        
        for (Texture texture : textures) {
            switch (texture.getTarget()) {
                case TWO_D -> tex2d++;
                case CUBEMAP -> {
                    if (cubemaps++ > 1) continue; //TODO add multi-cubemap warnings to MaterialTexture class once it actually exists
                    texture.bind(0);
                    //shader.setUniform("uCubeMap", 0, true); this is probably the only possible value anyway
                    continue;
                }
            }
            
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
            shader.setUniform("material." + texture.getType().name().toLowerCase() + number, validTextures, false);
        }
    
        boolean shininessMap = shininess > 0;
        shader.setUniform("uShininessMap", shininessMap, false);
        
        if (!shininessMap)
            shader.setUniform("material.shininess", material.getShininess(), false);
        shader.setUniform("material.specularity", material.getShininessStrength(), false);
    }
    
}
