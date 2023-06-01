package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.graphics.gl.ModelFactory;
import org.etieskrill.engine.graphics.gl.RawModelList;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.math.Vec2f;

public class Button extends Actor {
    
    public Button(Vec2f position, Vec2f size, float rotation) {
        super(position, size, rotation);
    }
    
    @Override
    public void draw(Renderer renderer, ModelFactory models) {
        RawModelList model = models.roundedRect(position.getX(), position.getY(), size.getX(), size.getY(), 1, 10);
        model.render(renderer);
        renderer.render(models.rectangle(100f, 100f, 50f, 200f));
    }
    
}
