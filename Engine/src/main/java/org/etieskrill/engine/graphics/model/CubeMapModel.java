package org.etieskrill.engine.graphics.model;

import org.etieskrill.engine.graphics.texture.Textures;

public class CubeMapModel extends Model {
    
    public CubeMapModel(String name) {
        super(new Model.Builder("cubemap.obj")
                .setName("cubemap")
                .setMaterials(
                        new Material.Builder()
                                .addTextures(
                                        Textures.getSkybox(name))
                                .build())
                .setCulling(false)
                .build());
    }
    
}
