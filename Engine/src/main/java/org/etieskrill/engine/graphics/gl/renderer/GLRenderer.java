package org.etieskrill.engine.graphics.gl.renderer;

import lombok.Getter;
import lombok.Setter;
import org.etieskrill.engine.common.ApplicationDisposed;
import org.etieskrill.engine.common.Disposable;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.component.TransformC;
import org.etieskrill.engine.graphics.Renderer;
import org.etieskrill.engine.graphics.TextRenderer;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.Shaders;
import org.etieskrill.engine.graphics.gl.shader.Shaders_OutlineShaderKt;
import org.etieskrill.engine.graphics.gl.shader.impl.MissingShader;
import org.etieskrill.engine.graphics.model.*;
import org.etieskrill.engine.graphics.pipeline.DrawMode;
import org.etieskrill.engine.graphics.pipeline.Pipeline;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.util.EngineShaderLoader;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.util.HashMap;
import java.util.Map;

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
        getBoxTransform().getPosition().set(position);
        getBoxTransform().getScale().set(size);
        _render(getBoxTransform(), getBox(), shader, combined);
    }

    //TODO update spec: all factory methods use loaders by default, constructors/builders do not
    @Getter(lazy = true)
    private static final Shaders.OutlineShader outlineShader = (Shaders.OutlineShader) EngineShaderLoader.INSTANCE.load("outline", Shaders::getOutlineShader);

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
        pipeline.getFrameBuffer().bind(); //TODO at least cache current framebuffer somewhere and check - minimising context switches after that is task of higher abstraction

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

        glEnable(GL_BLEND); //it's fucking crazy that blending is disabled by default

        int vertexCount;

        if (indexed) {
            vertexCount = pipeline.getVao().getIndexBuffer().getNumElements();
            glDrawElements(primitiveType, vertexCount, GL_UNSIGNED_INT, 0L);
        } else {
            if (pipeline.getVao() == null && pipeline.getVertexCount() == null) {
                throw new IllegalStateException("Pipeline without vertex array object must have vertex count set");
            }
            if (pipeline.getVao() != null) {
                vertexCount = pipeline.getVao().getVertexBuffer().getNumElements();
            } else {
                vertexCount = pipeline.getVertexCount();
            }
            glDrawArrays(primitiveType, 0, vertexCount);
        }

        setRenderCalls(getRenderCalls() + 1);
        setTrianglesDrawn(getTrianglesDrawn() + switch (pipeline.getConfig().getPrimitiveType()) {
            case POINTS, LINES, LINE_STRIP -> 0;
            case TRIANGLES -> vertexCount / 3;
            case TRIANGLE_STRIP -> vertexCount - 2;
        });
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
        shader.setUniform("view", camera.getView(), false);
        shader.setUniform("projection", camera.getProjection(), false);
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

        glEnable(GL_BLEND);

        int mode = shader instanceof Shaders.ShowNormalsShader ? GL_POINTS : mesh.getDrawMode().gl();
        if (!instanced) glDrawElements(mode, mesh.getVao().getNumElements(), GL_UNSIGNED_INT, 0);
        else glDrawElementsInstanced(mode, mesh.getVao().getNumElements(), GL_UNSIGNED_INT, 0, numInstances);

        var textureContext = getOrCreateShaderTextureContext(shader);
        textureContext.nextTexture = textureContext.manuallyBoundTextures + 1;

        resetMaterial(mesh.getMaterial());

        if (mesh.getDrawMode() == Mesh.DrawMode.TRIANGLES) {
            setTrianglesDrawn(getTrianglesDrawn() + mesh.getVao().getNumElements() / 3);
        }
        setRenderCalls(getRenderCalls() + 1);
    }

    private void bindMaterial(Material material, ShaderProgram shader) {
        if (material.isTwoSided()) glDisable(GL_CULL_FACE);

        //FIXME bind all unused samplers to unused slots to avoid sampler type conflict
        //FIXME if proper: bind default 1x1 texture to type if not set

//        AbstractTexture.clearBind(0); //TODO apparently sampler 0 is the default for unset uniform samplers, making some shaders sample into textures with arbitrary formats and values - potentially causing undefined behaviour render errors
//        for (int i = textureContext.nextTexture; i < MAX_USABLE_TEXTURE_UNIT; i++) { //TODO this is a little inefficient, but you don't have to unbind textures all the time like this
//            AbstractTexture.clearBind(i);
//        }

        shader.setUniform("material", material, false);
    }

    private void resetMaterial(Material material) {
        if (material.isTwoSided()) glEnable(GL_CULL_FACE);
    }

}
