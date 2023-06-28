package org.etieskrill.engine.scene._2d;

import glm.vec._3.Vec3;
import org.etieskrill.engine.graphics.gl.Batch;
import org.etieskrill.engine.graphics.gl.RawModelList;

public class Button extends Group {
    
    public Button() {
        super();
    }

    public Button(Layout layout) {
        super();
        setLayout(layout);
    }
    
    @Override
    public void draw(Batch batch) {
        //RawModelList model = batch.getModelFactory().roundedRect(position.getX(), position.getY(), size.getX(), size.getY(),
        //        size.getX() > size.getY() ? size.getY() / 4f : size.getX() / 4f, 10);
        //model.render(batch.getRenderer());

        //RawModel model = batch.getModelFactory().rectangle(position.getX(), position.getY(), size.getX(), size.getY());
        //Model _model = new Model(model);
        //_model.addTexture(new Texture("buff_bale.jpg"), 0);
        //batch.render(_model);

        //System.out.println("[" + toString() + "] " + position + " " + size);

        RawModelList _model = batch.getModelFactory().roundedRect(
                position.getX(), position.getY(), size.getX(), size.getY(),
                size.getX() > size.getY() ? size.getY() / 4f : size.getX() / 4f, 10);
        batch.getShader().start();
        batch.getShader().setUniformVec3("uColour", new Vec3(1f, 0f, 0f));
        batch.render(_model);
    }
    
    //@Override
    //public void update(double delta) {
    //}
    
    @Override
    public void layout() {
        shouldLayout = false;
    }

}
