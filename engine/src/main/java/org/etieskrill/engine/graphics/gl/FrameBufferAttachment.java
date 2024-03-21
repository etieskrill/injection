package org.etieskrill.engine.graphics.gl;

import org.joml.Vector2ic;

public interface FrameBufferAttachment {

    Vector2ic getSize();
    int getID();

    //TODO add bind() and unbind() methods with default impls

}
