package org.etieskrill.engine.graphics;

import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.component.TransformC;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.model.CubeMapModel;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.joml.Vector4fc;

/**
 * A {@code Renderer} provides an abstracted way to render a {@link Model} and its subclasses, such as
 * {@link CubeMapModel}. The rendering is done using a provided {@link ShaderProgram}, which may in some cases be
 * optional, and takes in the combined viewpoint transform of a {@link Camera}.
 */
public interface Renderer {

    /**
     * Only for the currently bound buffers: clears the buffers and prepares the renderer state for the next frame.
     */
    void prepare();

    /**
     * Advances the renderer to draw the next frame to the screen buffer. Should only be called once every frame.
     */
    void nextFrame();

    void bindNextFreeTexture(ShaderProgram shader, String name, AbstractTexture texture);

    /**
     * Render the {@link Model} at the {@link Transform} using the specified {@link ShaderProgram} and {@link Camera}.
     *
     * @param transform applied to the model
     * @param model     model to be rendered
     * @param shader    shader to use while rendering
     * @param camera    pov to render from
     */
    void render(TransformC transform, Model model, ShaderProgram shader, Camera camera);

    /**
     * <p>
     * Render the {@link Model} using the specified {@link ShaderProgram shader} and {@link Matrix4fc final transform},
     * either from {@link Camera#getCombined()}, or custom.
     * </p>
     * <p>
     * Primarily for internal implementations which do not necessarily need a camera.
     * </p>
     *
     * @param model    model to be rendered
     * @param shader   shader to use while rendering
     * @param combined camera view and perspective
     */
    void render(TransformC transform, Model model, ShaderProgram shader, Matrix4fc combined);

    /**
     * Render a number of instances specified by {@code numInstances} of the {@link Model} using the
     * {@link ShaderProgram shader} and combined {@link Camera camera} transform from
     * {@link Camera#getCombined() getCombined()}.
     *
     * @param model  model to be rendered
     * @param shader shader to use while rendering
     * @param camera pov to render from
     */
    //TODO idea: a shader may be an instancing shader with it's own validation and other mechanics, which is resolved
    // automatically while rendering, making this method obsolete
    void renderInstances(TransformC transform, Model model, int numInstances, ShaderProgram shader, Camera camera);

    void renderOutline(Model model, ShaderProgram shader, Camera camera, float thickness, Vector4fc colour, boolean writeToFront);

    void renderWireframe(TransformC transform, Model model, ShaderProgram shader, Camera camera);

    void render(CubeMapModel cubemap, ShaderProgram shader, Matrix4fc combined);


    //TODO move to another interface with more "primitive" drawing calls with backing singleton/per-renderer instances
    //TODO screenspace wrappers
    void renderBox(Vector3fc position, Vector3fc size, ShaderProgram shader, Matrix4fc combined);

}
