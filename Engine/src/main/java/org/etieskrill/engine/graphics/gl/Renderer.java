package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.graphics.model.*;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.font.Font;
import org.etieskrill.engine.graphics.texture.font.Glyph;
import org.joml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.Objects.requireNonNullElse;
import static org.lwjgl.opengl.GL33C.*;

public class Renderer {
    
    private static final float clearColour = 0.25f;//0.025f;
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    public void prepare() {
        glClearColor(clearColour, clearColour, clearColour, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    }
    
    public void render(Model model, ShaderProgram shader, Matrix4fc combined) {
        if (shader.isPlaceholder()) {
            renderOutline(model, shader, combined, 1f, new Vector4f(1, 0, 1, 1), true);
            return;
        }
        
        _render(model, shader, combined);
    }

    private Model box;

    private Model getBox() {
        if (box == null) box = ModelFactory.box(new Vector3f(1));
        return box;
    }

    public void renderBox(Vector3f position, Vector3f size, ShaderProgram shader, Matrix4fc combined) {
        getBox().getTransform().setPosition(position).setScale(size);
        _render(getBox(), shader, combined);
    }
    
    //TODO update spec: all factory methods use loaders by default, constructors/builders do not
    private static ShaderProgram outlineShader;
    private static ShaderProgram getOutlineShader() {
        if (outlineShader == null)
            outlineShader = Shaders.getOutlineShader();
        return outlineShader;
    }
    
    //TODO add outline & wireframe as flag in render
    public void renderOutline(Model model, ShaderProgram shader, Matrix4fc combined) {
        renderOutline(model, shader, combined, 0.5f, new Vector4f(1f, 0f, 0f, 1f), false);
    }
    
    public void renderOutline(Model model, ShaderProgram shader, Matrix4fc combined, float thickness, Vector4fc colour, boolean writeToFront) {
        getOutlineShader().setUniform("uThicknessFactor", thickness);
        getOutlineShader().setUniform("uColour", colour);
        
        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
    
        glStencilFunc(GL_ALWAYS, 1, 0xFF);
        glStencilMask(0xFF);
        _render(model, shader, combined);
    
        glStencilFunc(GL_NOTEQUAL, 1, 0xFF);
        glStencilMask(0x00);
        if (writeToFront) glDisable(GL_DEPTH_TEST);
        _render(model, getOutlineShader(), combined);
    
        glStencilFunc(GL_ALWAYS, 0, 0xFF);
        glStencilMask(0xFF);
        if (writeToFront) glEnable(GL_DEPTH_TEST);
    }
    
    public void renderWireframe(Model model, ShaderProgram shader, Matrix4fc combined) {
        glDisable(GL_CULL_FACE);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        _render(model, shader, combined);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glEnable(GL_CULL_FACE);
    }
    
    public void render(CubeMapModel cubemap, ShaderProgram shader, Matrix4fc combined) {
        //TODO perf increase use early depth test to discard instead of this
        glDepthMask(false);
        _render(cubemap, shader, combined);
        glDepthMask(true);
    }
    
    private static Model quad;
    private static Model getQuad() {
        if (quad == null)
            quad = ModelFactory
                    .rectangle(new Vector2f(0), new Vector2f(1))
                    .hasTransparency() //TODO when antialiasing is used, this could be disabled, depending on how aa works
                    .disableCulling()
                    .build();
        return quad;
    }
    
    public void render(Glyph glyph, Vector2fc position, ShaderProgram shader, Matrix4fc combined) {
        getQuad().getTransform()
                .setScale(new Vector3f(glyph.getSize(), 1))
                .setPosition(new Vector3f(position.add(glyph.getPosition(), new Vector2f()), 0));

        List<AbstractTexture> textures = getQuad().getNodes().getFirst().getMeshes().getFirst().getMaterial().getTextures();
        textures.clear();
        textures.add(glyph.getTexture());

        _render(getQuad(), shader, combined);
    }
    
    //TODO can be improved by rendering to some framebuffer and reusing unchanged sections instead of rendering every single glyph with a separate render call
    public void render(String chars, Font font, Vector2fc position, ShaderProgram shader, Matrix4fc combined) {
        Vector2f pen = new Vector2f(0);
        for (Glyph glyph : font.getGlyphs(chars)) {
            switch (requireNonNullElse(glyph.getCharacter(), (char) 0)) {
                case '\n' -> {
                    pen.set(0, pen.y() + font.getLineHeight());
                    continue;
                }
            }
            
            render(glyph, new Vector2f(position).add(pen), shader, combined);
            pen.add(glyph.getAdvance());
        }
    }
    
    private void _render(Model model, ShaderProgram shader, Matrix4fc combined) {
        shader.setUniform("uCombined", combined, false);
        shader.setUniform("uModel", model.getFinalTransform().getMatrix(), false);
        shader.setUniform("uNormal", model.getFinalTransform().getMatrix().invert(new Matrix4f()).transpose().get3x3(new Matrix3f()), false);

        if (!model.doCulling()) glDisable(GL_CULL_FACE);
        if (model.hasTransparency()) glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        shader.start();
        Node rootNode = model.getNodes().getFirst();
        renderNode(rootNode, shader, rootNode.getTransform().getMatrix());
        shader.stop();
        if (!model.doCulling()) glEnable(GL_CULL_FACE);
        if (model.hasTransparency()) glBlendFunc(GL_ONE, GL_ZERO);
    }
    
    //TODO should once again be made private, so dont use anywhere else, even if you **could**
    @Deprecated
    public void renderNode(Node node, ShaderProgram shader, Matrix4fc meshTransform) {
        shader.setUniform("uMesh", meshTransform, false);

        for (Mesh mesh : node.getMeshes())
            renderMesh(mesh, shader);

        for (Node child : node.getChildren())
            renderNode(child, shader, meshTransform.mul(child.getTransform().getMatrix(), new Matrix4f()));
    }

    private void renderMesh(Mesh mesh, ShaderProgram shader) {
        bindMaterial(mesh.getMaterial(), shader);
        glBindVertexArray(mesh.getVao());

        glDrawElements(mesh.getDrawMode().gl(), mesh.getNumIndices(), GL_UNSIGNED_INT, 0);

        AbstractTexture.unbindAllTextures();
        glBindVertexArray(0);
    }

    public void renderInstances(Model model, int numInstances, ShaderProgram shader, Matrix4fc combined) {
        shader.setUniform("uCombined", combined, false);
        shader.setUniform("uModel", model.getFinalTransform().getMatrix(), false);
        shader.setUniform("uNormal", model.getFinalTransform().getMatrix().invert(new Matrix4f()).transpose().get3x3(new Matrix3f()), false); //TODO store in fixed matrix to lessen object creation

        if (!model.doCulling()) glDisable(GL_CULL_FACE);
        if (model.hasTransparency()) glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        shader.start();
        Node rootNode = model.getNodes().getFirst();
        renderNodeInstances(rootNode, numInstances, shader, rootNode.getTransform().getMatrix());
        shader.stop();
        if (!model.doCulling()) glEnable(GL_CULL_FACE);
        if (model.hasTransparency()) glBlendFunc(GL_ONE, GL_ZERO);
    }

    public void renderNodeInstances(Node node, int numInstances, ShaderProgram shader, Matrix4fc meshTransform) {
        shader.setUniform("uMesh", meshTransform, false);

        for (Mesh mesh : node.getMeshes())
            renderMeshInstances(mesh, numInstances, shader);

        for (Node child : node.getChildren())
            renderNodeInstances(child, numInstances, shader, meshTransform.mul(child.getTransform().getMatrix(), new Matrix4f())); //TODO same as with normal, store precalculated in nodes
    }

    private void renderMeshInstances(Mesh mesh, int numInstances, ShaderProgram shader) {
        bindMaterial(mesh.getMaterial(), shader);
        glBindVertexArray(mesh.getVao());

        glDrawElementsInstanced(mesh.getDrawMode().gl(), mesh.getNumIndices(), GL_UNSIGNED_INT, 0, numInstances);

        AbstractTexture.unbindAllTextures();
        glBindVertexArray(0);
    }
    
    private void bindMaterial(Material material, ShaderProgram shader) {
        //TODO here the renderer could decide what kind of shader to use, based on the material given
        int tex2d = 0, cubemaps = 0;
        int diffuse = 0, specular = 0, normal = 0, emissive = 0, height = 0, shininess = 0;
        List<AbstractTexture> textures = material.getTextures();
        
        for (AbstractTexture texture : textures) {
            String uniform = "material.";
            int number = -1;
            
            switch (texture.getTarget()) {
                case TWO_D -> {
                    uniform += texture.getType().name().toLowerCase();
                    tex2d++;
                    number = switch (texture.getType()) {
                        case DIFFUSE -> diffuse++;
                        case SPECULAR -> specular++; //TODO could pass texture colour channels here, but it is probs best to just finally define a standard for this whole shebang
                        case NORMAL -> normal++;
                        case EMISSIVE -> emissive++;
                        case HEIGHT -> height++;
                        case SHININESS -> shininess++;
                        case UNKNOWN -> throw new IllegalStateException("Texture has invalid type");
                    };
                }
                case CUBEMAP -> {
                    uniform += "cubemap";
                    number = cubemaps++;
                }
            }
            
            int validTextures = (tex2d + cubemaps) - 1;
            texture.bind(validTextures);
            shader.setUniform(uniform + number, validTextures, false);
        }

        //TODO add a way to map all available material props automatically
        shader.setUniform("material.colour", material.getColourProperty(Material.Property.COLOUR_BASE), false);
        shader.setUniform("material.diffuseColour", material.getColourProperty(Material.Property.COLOUR_DIFFUSE), false);
        shader.setUniform("material.emissiveColour", material.getColourProperty(Material.Property.COLOUR_EMISSIVE), false);
        shader.setUniform("material.emissiveIntensity", material.getValueProperty(Material.Property.INTENSITY_EMISSIVE), false);
        shader.setUniform("material.opacity", material.getValueProperty(Material.Property.OPACITY), false);

        if (shininess == 0)
            shader.setUniform("material.shininess", material.getValueProperty(Material.Property.SHININESS), false);
        shader.setUniform("material.specularity", material.getValueProperty(Material.Property.SHININESS_STRENGTH), false);

        //Optional information
        shader.setUniform("material.numTextures", tex2d + cubemaps, false);
    }
    
}
