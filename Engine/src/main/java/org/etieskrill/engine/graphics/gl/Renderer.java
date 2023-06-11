package org.etieskrill.engine.graphics.gl;

import static org.lwjgl.opengl.GL33C.*;

public class Renderer {
    
    public void prepare() {
        glClearColor(0.025f, 0.025f, 0.025f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }
    
    public void render(RawModel model) {
        glBindVertexArray(model.getVao());
        if (model instanceof Model texModel) texModel.bind();
        
        if (model.hasIndexBuffer())
            glDrawElements(model.getDrawMode(), model.getNumVertices(), GL_UNSIGNED_SHORT, 0);
        else
            glDrawArrays(model.getDrawMode(), 0, model.getNumVertices());
        
        if (model instanceof Model) Model.unbind();
        glBindVertexArray(0);
    }
    
}
