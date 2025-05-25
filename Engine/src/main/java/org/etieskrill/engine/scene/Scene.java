package org.etieskrill.engine.scene;

import kotlin.jvm.functions.Function1;
import org.etieskrill.engine.graphics.Batch;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.input.Key;
import org.etieskrill.engine.input.KeyInputHandler;
import org.etieskrill.engine.input.Keys;
import org.etieskrill.engine.input.MouseGestureHandler;
import org.etieskrill.engine.scene.component.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector2ic;
import org.joml.Vector3f;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static org.lwjgl.opengl.GL11C.*;

public class Scene extends MouseGestureHandler implements KeyInputHandler {

    private @NotNull Batch batch;
    private @NotNull Node<?> root; //TODO direct root (e.g. label) is not formatted -> add transparent parent container?
    private @NotNull Camera camera;

    private final Vector2f size;

    private @Nullable Node<?> focusedNode;

    private final Function1<Node<?>, Boolean> focusCallback = node -> {
        focusedNode = requireNonNull(node);
        node.setFocused$engine(true);
        return true;
    };

    /**
     * Available for more convenient construction in subclasses. Take care to <b>set {@link Scene#batch},
     * {@link Scene#root} and {@link Scene#camera} using the setters</b> while still in the constructor.
     * Use at your own peril.
     */
    @SuppressWarnings("DataFlowIssue")
    protected Scene() {
        this.batch = null;
        this.root = null;
        this.camera = null;

        this.size = new Vector2f(0);
    }

    public Scene(@NotNull Batch batch, @NotNull Node<?> root, @NotNull Camera camera) {
        this.batch = batch;
        this.root = root; //shut up, data flow analysis
        setRoot(root);
        this.camera = camera;
        setCamera(camera);

        this.size = new Vector2f(0);

        this.focusedNode = root;
    }

    public void update(double delta) {
        root.getPosition().set(0);
        root.getSize().set(size);
        root.update(delta);

        root.format();
    }

    public void render() {
        if (!root.isVisible()) return;
        batch.setCombined(camera.getCombined());

        glDisable(GL_DEPTH_TEST); //TODO either this or implement with depth testing
        root.render(batch);
        glEnable(GL_DEPTH_TEST);
    }

    public @NotNull Batch getBatch() {
        return batch;
    }

    public void setBatch(@NotNull Batch batch) {
        this.batch = batch;
    }

    public @NotNull Node<?> getRoot() {
        return root;
    }

    public void setRoot(@NotNull Node<?> root) {
        if (this.root == focusedNode) focusedNode = root;
        this.root.setFocusRequestCallback(null);
        this.root.setFocused$engine(false);

        this.root = root;
        this.root.setFocusRequestCallback(focusCallback);
        this.root.setFocused$engine(true);
    }

    public @NotNull Camera getCamera() {
        return camera;
    }

    /**
     * Sets the camera used to view the scene.
     * <p>
     * This method resets the camera's transform to the standard ui viewport, with the origin in the top-left corner,
     * and the window size as the bottom-right corner.
     *
     * @param camera the camera to set this scene to
     */
    public void setCamera(@NotNull Camera camera) {
        this.camera = camera;
        this.camera.setRotation(0, 180, 0);
        this.camera.setPosition(new Vector3f(camera.getViewportSize(), 0).div(2));
    }

    public void show() {
        this.root.show();
    }

    public void hide() {
        this.root.hide();
    }

    public void setSize(Vector2ic size) {
        this.size.set(size);
        root.invalidate();
    }

    @Override
    public boolean invokeClick(Key button, @NotNull Keys.Action action, double posX, double posY) {
        super.invokeClick(button, action, posX, posY);
        return root.handleHit(button, action, posX, posY);
    }

    @Override
    public boolean invokeDrag(double deltaX, double deltaY, double posX, double posY) {
        return root.handleDrag(deltaX, deltaY, posX, posY);
    }

    @Override
    public boolean invoke(Key.Type type, int key, int action, int modifiers) {
        return root.handleKey(new Key(type, key, modifiers), requireNonNull(Keys.Action.fromGLFW(action)));
    }

    @Override
    public boolean invokeCharacter(char character) {
        return requireNonNullElse(focusedNode, root).handleCharacter(character);
    }

}
