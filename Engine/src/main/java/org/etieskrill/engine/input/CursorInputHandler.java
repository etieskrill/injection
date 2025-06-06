package org.etieskrill.engine.input;

public interface CursorInputHandler {

    boolean invokeClick(Key button, Keys.Action action, double posX, double posY);

    boolean invokeMove(double posX, double posY);

    boolean invokeScroll(double deltaX, double deltaY);

}
