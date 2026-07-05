package org.etieskrill.engine.input;

import org.jetbrains.annotations.NotNull;

public interface CursorInputHandler {

    boolean invokeClick(@NotNull Key button, @NotNull Keys.Action action, double posX, double posY);

    boolean invokeMove(double posX, double posY);

    boolean invokeScroll(double deltaX, double deltaY);

}
