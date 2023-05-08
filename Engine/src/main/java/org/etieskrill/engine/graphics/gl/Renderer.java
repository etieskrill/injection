package org.etieskrill.engine.graphics.gl;

import org.lwjgl.opengl.GL33C;

public class Renderer {
    
    public void prepare() {
        GL33C.glClearColor(0f, 0f, 0f, 1f);
        GL33C.glClear(GL33C.GL_COLOR_BUFFER_BIT | GL33C.GL_DEPTH_BUFFER_BIT);
    }
    
    public void render(RawModel model) {
        GL33C.glBindVertexArray(model.getVao());
        //GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, /*model.getTextures()*/);
        GL33C.glDrawElements(model.getDrawMode(), model.getNumVertices(), GL33C.GL_UNSIGNED_SHORT, 0);
        //GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, 0);
        GL33C.glBindVertexArray(0);
    }

    public void renderPrimitive(RawModel model) {
        GL33C.glBindVertexArray(model.getVao());
        GL33C.glDrawArrays(model.getDrawMode(), 0, model.getNumVertices());
        GL33C.glBindVertexArray(0);
    }
    
}
