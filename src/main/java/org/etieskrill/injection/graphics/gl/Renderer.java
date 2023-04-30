package org.etieskrill.injection.graphics.gl;

import static org.lwjgl.opengl.GL30C.*;

public class Renderer {
    
    public void prepare() {
        glClearColor(0.5f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT);
    }
    
    public void render(RawMemoryModel model) {
        glBindVertexArray(model.getVao());
        glEnableVertexAttribArray(0);
        glDrawElements(GL_TRIANGLES, model.getNumVertices(), GL_UNSIGNED_INT, 0);
        glDisableVertexAttribArray(0);
        glBindVertexArray(0);
    }
    
}
