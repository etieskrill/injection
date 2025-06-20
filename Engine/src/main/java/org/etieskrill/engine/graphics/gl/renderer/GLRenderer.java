package org.etieskrill.engine.graphics.gl.renderer;

import lombok.Getter;
import lombok.Setter;
import org.etieskrill.engine.ApplicationDisposed;
import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.component.TransformC;
import org.etieskrill.engine.graphics.Renderer;
import org.etieskrill.engine.graphics.TextRenderer;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.Shaders;
import org.etieskrill.engine.graphics.gl.shader.Shaders_OutlineShaderKt;
import org.etieskrill.engine.graphics.gl.shader.impl.MissingShader;
import org.etieskrill.engine.graphics.model.*;
import org.etieskrill.engine.graphics.pipeline.DrawMode;
import org.etieskrill.engine.graphics.pipeline.Pipeline;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.util.Loaders;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.etieskrill.engine.graphics.model.Material.Property.*;
import static org.lwjgl.opengl.GL33C.*;

//TODO assure thread safety/passing
//TODO separate text renderer
public class GLRenderer extends GLTextRenderer implements Renderer, TextRenderer, Disposable {

    private static final float CLEAR_COLOUR = 0.25f;//0.025f;
    private @Setter Vector4f clearColour = new Vector4f(CLEAR_COLOUR, CLEAR_COLOUR, CLEAR_COLOUR, 1);
    private static final int MAX_USABLE_TEXTURE_UNIT = 8; //TODO make more configurable

    private final Map<ShaderProgram, ShaderTextureContext> textureContexts = new HashMap<>();

    private static class ShaderTextureContext {
        int nextTexture = 1;
        int manuallyBoundTextures = 0;
    }

    @Override
    public void prepare() {
        textureContexts.forEach((shader, textureContext) -> {
            textureContext.nextTexture = 1;
            textureContext.manuallyBoundTextures = 0;
        });
    }

    @Override
    public void nextFrame() {
        FrameBuffer.bindScreenBuffer();
        FrameBuffer.clearScreenBuffer(clearColour);
        prepare();

        queryGpuTime();
        resetCounters();
    }

    //TODO consider moving to shader
    //TODO override existing bind if same name
    @Override
    public void bindNextFreeTexture(ShaderProgram shader, String name, AbstractTexture texture) {
        var textureContext = getOrCreateShaderTextureContext(shader);
        texture.bind(textureContext.nextTexture);
        shader.setUniform(name, textureContext.nextTexture++, false);
        textureContext.manuallyBoundTextures++;
    }

    private ShaderTextureContext getOrCreateShaderTextureContext(ShaderProgram shader) {
        var textureContext = textureContexts.get(shader);

        if (textureContext == null) {
            var newContext = new ShaderTextureContext();
            textureContexts.put(shader, newContext);
            return newContext;
        }

        return textureContext;
    }

    @Override
    public void render(TransformC transform, Model model, ShaderProgram shader, Camera camera) {
        if (shader instanceof MissingShader) {
            renderOutline(model, shader, camera, 1f, new Vector4f(1, 0, 1, 1), true);
            return;
        }

        _render(transform, model, shader, camera);
    }

    @Override
    public void render(TransformC transform, Model model, ShaderProgram shader, Matrix4fc combined) {
        //TODO invalid indicator - speaking of; should invalid models/shaders even be rendered? i think so (it'll be easily toggleable later)
        _render(transform, model, shader, combined);
    }

    @ApplicationDisposed
    @Getter(lazy = true)
    private final Model box = ModelFactory.box(new Vector3f(1));
    private final @Getter(lazy = true) Transform boxTransform = new Transform();

    @Override
    public void renderBox(Vector3fc position, Vector3fc size, ShaderProgram shader, Matrix4fc combined) {
        getBoxTransform().setPosition(position).setScale(size);
        _render(getBoxTransform(), getBox(), shader, combined);
    }

    //TODO update spec: all factory methods use loaders by default, constructors/builders do not
    @Getter(lazy = true)
    private static final Shaders.OutlineShader outlineShader = (Shaders.OutlineShader) Loaders.ShaderLoader.get().load("outline", Shaders::getOutlineShader);

    //TODO add outline & wireframe as flag in render
    public void renderOutline(Model model, ShaderProgram shader, Camera camera) {
        renderOutline(model, shader, camera, 0.5f, new Vector4f(1f, 0f, 0f, 1f), false);
    }

    @Override
    public void renderOutline(Model model, ShaderProgram shader, Camera camera, float thickness, Vector4fc colour, boolean writeToFront) {
        //FIXME import outer classes in accessors and discard this shit
        Shaders_OutlineShaderKt.setThicknessFactor(getOutlineShader(), thickness);
        Shaders_OutlineShaderKt.setColour(getOutlineShader(), (Vector4f) colour);

        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);

        glStencilFunc(GL_ALWAYS, 1, 0xFF);
        glStencilMask(0xFF);
//        _render(model, shader, combined);

        glStencilFunc(GL_NOTEQUAL, 1, 0xFF);
        glStencilMask(0x00);
        if (writeToFront) glDisable(GL_DEPTH_TEST);
//        _render(model, getOutlineShader(), combined);

        glStencilFunc(GL_ALWAYS, 0, 0xFF);
        glStencilMask(0xFF);
        if (writeToFront) glEnable(GL_DEPTH_TEST);
    }

    @Override
    public void renderWireframe(TransformC transform, Model model, ShaderProgram shader, Camera camera) {
        glDisable(GL_CULL_FACE);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        _render(transform, model, shader, camera);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glEnable(GL_CULL_FACE);
    }

    @Override
    public void render(CubeMapModel cubemap, ShaderProgram shader, Matrix4fc combined) {
        glDepthMask(false);
        glBlendFunc(GL_ONE, GL_ZERO);
        _render(null, cubemap, shader, combined);
        glDepthMask(true);
    }

    private static int dummyVAO = -1;

    public static int getDummyVAO() {
        if (dummyVAO == -1) dummyVAO = glGenVertexArrays();
        return dummyVAO;
    }

    @Override
    public void render(Pipeline<?> pipeline) {
        if (pipeline.getFrameBuffer() != null) {
            pipeline.getFrameBuffer().bind(); //TODO at least cache current framebuffer somewhere and check - minimising context switches after that is task of higher abstraction
        } else {
            FrameBuffer.bindScreenBuffer();
        }

        boolean indexed;
        if (pipeline.getVao() != null) {
            pipeline.getVao().bind();
            indexed = pipeline.getVao().isIndexed();
        } else {
            glBindVertexArray(getDummyVAO());
            indexed = false;
        }

        pipeline.getShader().start();

        switch (pipeline.getConfig().getAlphaMode()) {
            case OPAQUE -> glBlendFunc(GL_ONE, GL_ZERO);
            case SOURCE_ALPHA -> glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        }

        int primitiveType = switch (pipeline.getConfig().getPrimitiveType()) {
            case POINTS -> GL_POINTS;
            case LINES -> GL_LINES;
            case LINE_STRIP -> GL_LINE_STRIP;
            case TRIANGLES -> GL_TRIANGLES;
            case TRIANGLE_STRIP -> GL_TRIANGLE_STRIP;
        };

        switch (pipeline.getConfig().getCullingMode()) {
            case NONE -> glDisable(GL_CULL_FACE);
            case BACK -> {
                glEnable(GL_CULL_FACE);
                glCullFace(GL_BACK);
            }
            case FRONT -> {
                glEnable(GL_CULL_FACE);
                glCullFace(GL_FRONT);
            }
            case FRONT_AND_BACK -> {
                glEnable(GL_CULL_FACE);
                glCullFace(GL_FRONT_AND_BACK);
            }
        }

        if (pipeline.getConfig().getDepthTest()) {
            glEnable(GL_DEPTH_TEST);
        } else {
            glDisable(GL_DEPTH_TEST);
        }

        glDepthMask(pipeline.getConfig().getWriteDepth());

        glPolygonMode(GL_FRONT, toGLPolygonMode(pipeline.getConfig().getFrontFaceDrawMode()));
        glPolygonMode(GL_BACK, toGLPolygonMode(pipeline.getConfig().getBackFaceDrawMode()));

        glPointSize(pipeline.getConfig().getPointSize());

        glLineWidth(pipeline.getConfig().getLineWidth());
        if (pipeline.getConfig().getLineAntiAliasing()) {
            glEnable(GL_LINE_SMOOTH);
        } else {
            glDisable(GL_LINE_SMOOTH);
        }

        int vertexCount;

        if (indexed) {
            vertexCount = pipeline.getVao().getIndexBuffer().getSize();
            glDrawElements(primitiveType, vertexCount, GL_UNSIGNED_INT, 0L);
        } else {
            if (pipeline.getVao() == null && pipeline.getVertexCount() == null) {
                throw new IllegalStateException("Pipeline without vertex array object must have vertex count set");
            }
            if (pipeline.getVao() != null) {
                vertexCount = pipeline.getVao().getVertexBuffer().getSize();
            } else {
                vertexCount = pipeline.getVertexCount();
            }
            glDrawArrays(primitiveType, 0, vertexCount);
        }

        renderCalls++;
        trianglesDrawn += switch (pipeline.getConfig().getPrimitiveType()) {
            case POINTS, LINES, LINE_STRIP -> 0;
            case TRIANGLES -> vertexCount / 3;
            case TRIANGLE_STRIP -> vertexCount - 2;
        };
    }

    private static int toGLPolygonMode(DrawMode mode) {
        return switch (mode) {
            case POINT -> GL_POINT;
            case LINE -> GL_LINE;
            case FILL -> GL_FILL;
        };
    }

    @Override
    public void renderInstances(TransformC transform,
                                Model model,
                                int numInstances,
                                ShaderProgram shader,
                                Camera camera) {
        _render(transform, model, shader, camera, true, numInstances);
    }

    private void _render(TransformC transform, Model model, ShaderProgram shader, Camera camera) {
        _render(transform, model, shader, camera, false, 0);
    }

    private void _render(TransformC transform, Model model, ShaderProgram shader, Matrix4fc combined) {
        _render(transform, model, shader, combined, false, 0);
    }

    private void _render(TransformC transform, Model model, ShaderProgram shader, Matrix4fc combined, boolean instanced, int numInstances) {
        shader.setUniform("combined", combined, false);
        shader.setUniform("invCombined", combined.invert(new Matrix4f()), false);
        _render(transform, model, shader, instanced, numInstances);
    }

    private void _render(TransformC transform, Model model, ShaderProgram shader, Camera camera, boolean instanced, int numInstances) {
        shader.setUniform("combined", camera.getCombined(), false); //TODO leave be?
        shader.setUniform("invCombined", camera.getInvCombined(), false);
        shader.setUniform("camera", camera, false);
        _render(transform, model, shader, instanced, numInstances);
    }

    private void _render(@Nullable TransformC transform, Model model, ShaderProgram shader, boolean instanced, int numInstances) {
        if (transform != null) {
            try (MemoryStack stack = MemoryStack.stackPush()) { //FIXME this literally does jackshit
                Matrix4f transformMatrix = new Matrix4f(transform.getMatrix());// = new Matrix4f(model.getFinalTransform().getMatrix().get(stack.callocFloat(16)));
                shader.setUniform("model", transformMatrix, false);
                shader.setUniform("normal", transformMatrix.invert().transpose().get3x3(new Matrix3f(stack.callocFloat(9))), false);
            }
        }

        //TODO move culling and transparency to and do per mesh, detect transparency in material loader via properties or based on if diffuse has alpha channel
        if (!model.doCulling()) glDisable(GL_CULL_FACE);
        if (model.hasTransparency()) glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        shader.start();
        Node rootNode = model.getNodes().getFirst();
        renderNode(rootNode, shader, rootNode.getTransform().getMatrix(), instanced, numInstances);
        if (!model.doCulling()) glEnable(GL_CULL_FACE);
        if (model.hasTransparency()) glBlendFunc(GL_ONE, GL_ZERO);
    }

    private void renderNode(Node node, ShaderProgram shader, Matrix4fc meshTransform, boolean instanced, int numInstances) {
        shader.setUniform("mesh", meshTransform, false);

        for (Mesh mesh : node.getMeshes())
            renderMesh(mesh, shader, instanced, numInstances);

        for (Node child : node.getChildren())
            renderNode(child, shader, meshTransform.mul(child.getTransform().getMatrix(), new Matrix4f()), instanced, numInstances);
    }

    private void renderMesh(Mesh mesh, ShaderProgram shader, boolean instanced, int numInstances) {
        bindMaterial(mesh.getMaterial(), shader);
        mesh.getVao().bind();

        int mode = shader instanceof Shaders.ShowNormalsShader ? GL_POINTS : mesh.getDrawMode().gl();
        if (!instanced) glDrawElements(mode, mesh.getNumIndices(), GL_UNSIGNED_INT, 0);
        else glDrawElementsInstanced(mode, mesh.getNumIndices(), GL_UNSIGNED_INT, 0, numInstances);

        var textureContext = getOrCreateShaderTextureContext(shader);
        textureContext.nextTexture = textureContext.manuallyBoundTextures + 1;

        resetMaterial(mesh.getMaterial());

        if (mesh.getDrawMode() == Mesh.DrawMode.TRIANGLES)
            trianglesDrawn += mesh.getNumIndices() / 3;
        renderCalls++;
    }

    private void bindMaterial(Material material, ShaderProgram shader) {
        //TODO here the renderer could decide what kind of shader to use, based on the material given
        int diffuse = 0, specular = 0, normal = 0, emissive = 0, height = 0, shininess = 0, shadow = 0;
        List<AbstractTexture> textures = material.getTextures();

        var textureContext = getOrCreateShaderTextureContext(shader);
        if (textures.size() + textureContext.manuallyBoundTextures + 1 > MAX_USABLE_TEXTURE_UNIT) {
            throw new UnsupportedOperationException(
                    "No more than " + MAX_USABLE_TEXTURE_UNIT + " textures may be used for now");
        }

        //TODO requires some more investigation:
        //binding seemingly any texture object to slot 0 causes the context to throw a fit
//        for (int i = 0; i < MAX_USABLE_TEXTURE_UNIT; i++) {
//            if (i == 1) continue;
//            glActiveTexture(GL_TEXTURE0 + i);
//            glBindTexture(GL_TEXTURE_CUBE_MAP_ARRAY, 0);
//        }
//        glActiveTexture(GL_TEXTURE0);
//        glBindTexture(GL_TEXTURE_2D, 0);

        for (AbstractTexture texture : textures) {
            String uniform = "material.";
            int number = -1;

            switch (texture.getTarget()) {
                case TWO_D -> {
                    uniform += texture.getType().name().toLowerCase();
                    number = switch (texture.getType()) {
                        case DIFFUSE -> diffuse++;
                        case SPECULAR ->
                                specular++; //TODO could pass texture colour channels here, but it is probs best to just finally define a standard for this whole shebang
                        case NORMAL -> normal++;
                        case EMISSIVE -> emissive++;
                        case HEIGHT -> height++;
                        case SHININESS -> shininess++;
                        case SHADOW -> shadow++;
                        case UNKNOWN -> throw new IllegalStateException("Texture has invalid type");
                        default -> 0; //FIXME
                    };
                }
                case ARRAY -> uniform += "array";
                case CUBEMAP -> uniform += "cubemap";
            }

            texture.bind(textureContext.nextTexture);
            shader.setUniform(uniform + number, textureContext.nextTexture++, false);
        }

        AbstractTexture.clearBind(0); //TODO apparently sampler 0 is the default for unset uniform samplers, making some shaders sample into textures with arbitrary formats and values - potentially causing undefined behaviour render errors
        for (int i = textureContext.nextTexture; i < MAX_USABLE_TEXTURE_UNIT; i++) { //TODO this is a little inefficient, but you don't have to unbind textures all the time like this
            AbstractTexture.clearBind(i);
        }

        shader.setUniform("normalMapped", normal > 0, false);
        shader.setUniform("material.hasNormalMap", normal > 0, false);

        //TODO add a way to map all available material props automatically with sensible default values
        //TODO add invalidation flag to uniform properties and update here only if invalid (do not forget about the first time)
        shader.setUniform("material.colour", material.getColourProperty(COLOUR_BASE), false);
        shader.setUniform("material.diffuseColour", material.getColourProperty(COLOUR_DIFFUSE), false);
        shader.setUniform("material.emissiveColour", material.getColourProperty(COLOUR_EMISSIVE), false);
        shader.setUniform("material.emissiveIntensity", material.getPropertyOrDefault(INTENSITY_EMISSIVE, 0), false);
        shader.setUniform("material.opacity", material.getPropertyOrDefault(OPACITY, 1), false);

        shader.setUniform("material.hasDiffuse", diffuse > 0, false);
        shader.setUniform("material.specularTexture", specular > 0, false);
        shader.setUniform("material.emissiveTexture", emissive > 0, false);
        if (shininess == 0) //TODO this property thingies NEED type safety
            shader.setUniform("material.shininess", (float) material.getPropertyOrDefault(SHININESS, 64f), false);
        shader.setUniform("material.specularity", (float) material.getPropertyOrDefault(SHININESS_STRENGTH, 1f), false);

        shader.setUniform("material.colourDiffuse", material.getColourProperty(COLOUR_DIFFUSE), false);

        //Optional information
        shader.setUniform("material.numTextures", textureContext.nextTexture, false); //TODO not accurate anymore if binding textures manually after this

        if (material.getPropertyOrDefault(TWO_SIDED, false)) {
            glDisable(GL_CULL_FACE);
        }
    }

    private void resetMaterial(Material material) {
        if (material.getPropertyOrDefault(TWO_SIDED, false)) {
            glEnable(GL_CULL_FACE);
        }
    }

}
