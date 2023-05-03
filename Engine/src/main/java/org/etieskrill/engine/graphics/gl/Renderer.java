package org.etieskrill.engine.graphics.gl;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL45C;

public class Renderer {
    
    public void prepare() {
        GL11C.glClearColor(0f, 0f, 0f, 1f);
        GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT);
    }
    
    public void render(RawModel model) {
        GL30C.glBindVertexArray(model.getVao());
        //GL20C.glEnableVertexAttribArray(0); //so this is not needed either, right? i am a fucking wizard
        GL11C.glDrawElements(model.getDrawMode(), model.getNumVertices(), GL11C.GL_UNSIGNED_SHORT, 0);
        //GL20C.glDisableVertexAttribArray(0); //this is stored in the vao anyway is it not
        GL30C.glBindVertexArray(0);
    }
    
}
