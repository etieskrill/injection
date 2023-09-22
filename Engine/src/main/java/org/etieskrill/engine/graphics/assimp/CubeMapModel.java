package org.etieskrill.engine.graphics.assimp;

import org.etieskrill.engine.graphics.gl.CubeMapTexture;

public class CubeMapModel extends Model {
    
    public CubeMapModel(String name) {
        super(new Model.Builder("cubemap.obj")
                .setName("cubemap")
                .setMaterials(
                        new Material.Builder()
                                .addTextures(
                                        CubeMapTexture.getSkybox(name))
                                .build())
                .disableCulling()
                .build());
    }
    
}
