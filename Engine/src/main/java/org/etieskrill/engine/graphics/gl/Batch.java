package org.etieskrill.engine.graphics.gl;

public class Batch {
    
    private final Renderer renderer;
    private final ModelFactory models;
    
    public Batch(Renderer renderer, ModelFactory models) {
        this.renderer = renderer;
        this.models = models;
    }
    
    public Renderer getRenderer() {
        return renderer;
    }
    
    public ModelFactory getModelFactory() {
        return models;
    }
    
}
