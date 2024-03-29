package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.graphics.Renderer;
import org.etieskrill.engine.graphics.TextRenderer;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.graphics.model.*;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.util.List;

import static org.lwjgl.opengl.GL33C.*;

//TODO assure thread safety/passing
//TODO separate text renderer
public class GLRenderer extends GLTextRenderer implements Renderer, TextRenderer {

    private static final float CLEAR_COLOUR = 0.25f;//0.025f;

    @Override
    public void prepare() {
        if (queryGpuTime) queryGpuTime();
        else {
            gpuTime = 0;
            averagedGpuTime = 0;
            gpuDelay = 0;
        }

        glClearColor(CLEAR_COLOUR, CLEAR_COLOUR, CLEAR_COLOUR, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        lastTrianglesDrawn = trianglesDrawn;
        trianglesDrawn = 0;
        lastRenderCalls = renderCalls;
        renderCalls = 0;
    }

    @Override
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

    @Override
    public void renderBox(Vector3fc position, Vector3fc size, ShaderProgram shader, Matrix4fc combined) {
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

    @Override
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

    @Override
    public void renderWireframe(Model model, ShaderProgram shader, Matrix4fc combined) {
        glDisable(GL_CULL_FACE);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        _render(model, shader, combined);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glEnable(GL_CULL_FACE);
    }

    @Override
    public void render(CubeMapModel cubemap, ShaderProgram shader, Matrix4fc combined) {
        //TODO perf increase use early depth test to discard instead of this
        glDepthMask(false);
        _render(cubemap, shader, combined);
        glDepthMask(true);
    }

    @Override
    public void renderInstances(Model model, int numInstances, ShaderProgram shader, Matrix4fc combined) {
        _render(model, shader, combined, true, numInstances);
    }

    private void _render(Model model, ShaderProgram shader, Matrix4fc combined) {
        _render(model, shader, combined, false, 0);
    }

    private void _render(Model model, ShaderProgram shader, Matrix4fc combined, boolean instanced, int numInstances) {
        shader.setUniform("uCombined", combined, false);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Matrix4f transform = new Matrix4f(model.getFinalTransform().getMatrix().get(stack.callocFloat(16)));
            shader.setUniform("uModel", transform, false);
            shader.setUniform("uNormal", transform.invert().transpose().get3x3(new Matrix3f(stack.callocFloat(9))), false);
        }

        if (!model.doCulling()) glDisable(GL_CULL_FACE);
        if (model.hasTransparency()) glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        shader.start();
        Node rootNode = model.getNodes().getFirst();
        renderNode(rootNode, shader, rootNode.getTransform().getMatrix(), instanced, numInstances);
        if (!model.doCulling()) glEnable(GL_CULL_FACE);
        if (model.hasTransparency()) glBlendFunc(GL_ONE, GL_ZERO);
    }

    private void renderNode(Node node, ShaderProgram shader, Matrix4fc meshTransform, boolean instanced, int numInstances) {
        shader.setUniform("uMesh", meshTransform, false);

        for (Mesh mesh : node.getMeshes())
            renderMesh(mesh, shader, instanced, numInstances);

        for (Node child : node.getChildren())
            renderNode(child, shader, meshTransform.mul(child.getTransform().getMatrix(), new Matrix4f()), instanced, numInstances);
    }

    private void renderMesh(Mesh mesh, ShaderProgram shader, boolean instanced, int numInstances) {
        bindMaterial(mesh.getMaterial(), shader);
        glBindVertexArray(mesh.getVao());

        if (!instanced) glDrawElements(mesh.getDrawMode().gl(), mesh.getNumIndices(), GL_UNSIGNED_INT, 0);
        else glDrawElementsInstanced(mesh.getDrawMode().gl(), mesh.getNumIndices(), GL_UNSIGNED_INT, 0, numInstances);
        if (mesh.getDrawMode() == Mesh.DrawMode.TRIANGLES)
            trianglesDrawn += mesh.getNumIndices() / 3;
        renderCalls++;
    }

    private void bindMaterial(Material material, ShaderProgram shader) {
        //TODO here the renderer could decide what kind of shader to use, based on the material given
        int tex2d = 0, texArrays = 0, cubemaps = 0;
        int diffuse = 0, specular = 0, normal = 0, emissive = 0, height = 0, shininess = 0;
        List<AbstractTexture> textures = material.getTextures();
        int validTextures = 0;

        for (AbstractTexture texture : textures) {
            String uniform = "material.";
            int number = -1;

            switch (texture.getTarget()) {
                case TWO_D -> {
                    uniform += texture.getType().name().toLowerCase();
                    tex2d++;
                    number = switch (texture.getType()) {
                        case DIFFUSE -> diffuse++;
                        case SPECULAR ->
                                specular++; //TODO could pass texture colour channels here, but it is probs best to just finally define a standard for this whole shebang
                        case NORMAL -> normal++;
                        case EMISSIVE -> emissive++;
                        case HEIGHT -> height++;
                        case SHININESS -> shininess++;
                        case UNKNOWN -> throw new IllegalStateException("Texture has invalid type");
                    };
                }
                case ARRAY -> {
                    uniform += "array";
                    number = texArrays++;
                }
                case CUBEMAP -> {
                    uniform += "cubemap";
                    number = cubemaps++;
                }
            }

            validTextures = (tex2d + texArrays + cubemaps) - 1;
            texture.bind(validTextures);
            shader.setUniform(uniform + number, validTextures, false);
        }

        for (int i = validTextures + 1; i < 8; i++) //TODO this is a little inefficient, but you don't have to unbind textures all the time like this
            AbstractTexture.clearBind(i);

        //TODO add a way to map all available material props automatically
        shader.setUniform("material.colour", material.getColourProperty(Material.Property.COLOUR_BASE), false);
        shader.setUniform("material.diffuseColour", material.getColourProperty(Material.Property.COLOUR_DIFFUSE), false);
        shader.setUniform("material.emissiveColour", material.getColourProperty(Material.Property.COLOUR_EMISSIVE), false);
        shader.setUniform("material.emissiveIntensity", material.getValueProperty(Material.Property.INTENSITY_EMISSIVE), false);
        shader.setUniform("material.opacity", material.getValueProperty(Material.Property.OPACITY), false);

        if (shininess == 0)
            shader.setUniform("material.shininess", material.getValueProperty(Material.Property.SHININESS), false);
        shader.setUniform("material.specularity", (float)(int) material.getValueProperty(Material.Property.SHININESS_STRENGTH), false);

        //Optional information
        shader.setUniform("material.numTextures", tex2d + texArrays + cubemaps, false);
    }

}
