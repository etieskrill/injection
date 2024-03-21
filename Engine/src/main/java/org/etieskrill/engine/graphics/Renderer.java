package org.etieskrill.engine.graphics;

import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.graphics.model.CubeMapModel;
import org.etieskrill.engine.graphics.model.Model;
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
     * Clears and prepares the scene for the next frame.
     */
    void prepare();

    /**
     * Render the {@link Model} using the specified {@link ShaderProgram shader} and combined {@link Camera camera}
     * transform from {@link Camera#getCombined() getCombined()}.
     *
     * @param model    model to be rendered
     * @param shader   shader to use while rendering
     * @param combined camera view + perspective
     */
    void render(Model model, ShaderProgram shader, Matrix4fc combined);

    /**
     * Render a number of instances specified by {@code numInstances} of the {@link Model} using the
     * {@link ShaderProgram shader} and combined {@link Camera camera} transform from
     * {@link Camera#getCombined() getCombined()}.
     *
     * @param model    model to be rendered
     * @param shader   shader to use while rendering
     * @param combined camera view + perspective
     */
    //TODO idea: a shader may be an instancing shader with it's own validation and other mechanics, which is resolved
    // automatically while rendering, making this method obsolete
    void renderInstances(Model model, int numInstances, ShaderProgram shader, Matrix4fc combined);

    void renderOutline(Model model, ShaderProgram shader, Matrix4fc combined, float thickness, Vector4fc colour, boolean writeToFront);

    void renderWireframe(Model model, ShaderProgram shader, Matrix4fc combined);

    void render(CubeMapModel cubemap, ShaderProgram shader, Matrix4fc combined);


    //TODO move to another interface with more "primitive" drawing calls with backing singleton/per-renderer instances
    void renderBox(Vector3fc position, Vector3fc size, ShaderProgram shader, Matrix4fc combined);

    // ----------- Debugging / Statistics -----------

    int getTrianglesDrawn();

    int getRenderCalls();

    boolean doesQueryGpuTime();

    void setQueryGpuTime(boolean queryGpuTime);

    long getGpuTime();

    long getAveragedGpuTime();

    long getGpuDelay();

}
