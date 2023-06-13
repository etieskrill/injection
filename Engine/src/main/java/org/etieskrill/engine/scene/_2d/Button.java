package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.graphics.gl.Batch;
import org.etieskrill.engine.graphics.gl.RawModel;
import org.etieskrill.engine.graphics.gl.RawModelList;
import org.etieskrill.engine.math.Vec2f;

public class Button extends LayoutNode {
    
    public Button(Vec2f size) {
        super(size);
    }
    
    @Override
    public void draw(Batch batch) {
        //RawModelList model = batch.getModelFactory().roundedRect(position.getX(), position.getY(), size.getX(), size.getY(),
        //        size.getX() > size.getY() ? size.getY() / 4f : size.getX() / 4f, 10);
        //model.render(batch.getRenderer());
        RawModel model = batch.getModelFactory().rectangle(position.getX(), position.getY(), size.getX(), size.getY());
        batch.getRenderer().render(model);
    }
    
    @Override
    public void update(double delta) {
    }
    
    @Override
    public void layout() {
        shouldLayout = false;
    }
    
}
