package org.etieskrill.engine.graphics.gl;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

public class Renderer {
    
    public void prepare() {
        GL11C.glClearColor(0.5f, 0f, 0f, 1f);
        GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT);
    }
    
    public void render(RawMemoryModel model) {
        GL30C.glBindVertexArray(model.getVao());
        GL20C.glEnableVertexAttribArray(0);
        GL11C.glDrawElements(model.getDrawMode(), model.getNumVertices(), GL11C.GL_UNSIGNED_INT, 0);
        GL20C.glDisableVertexAttribArray(0);
        GL30C.glBindVertexArray(0);
    }
    
}
