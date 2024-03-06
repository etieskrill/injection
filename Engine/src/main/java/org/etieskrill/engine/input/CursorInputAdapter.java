package org.etieskrill.engine.input;

public interface CursorInputAdapter extends CursorInputHandler {

    @Override
    default boolean invokeClick(Key button, int action, double posX, double posY) {
        return false;
    }

    @Override
    default boolean invokeMove(double posX, double posY) {
        return false;
    }

    @Override
    default boolean invokeScroll(double deltaX, double deltaY) {
        return false;
    }

}
