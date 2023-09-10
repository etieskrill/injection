package org.etieskrill.engine.graphics.assimp;

import org.etieskrill.engine.graphics.gl.Texture;

import static org.etieskrill.engine.graphics.gl.CubeMapTexture.*;

public class CubeMapModel extends Model {
    
    public CubeMapModel(String name) {
        super(new Model.Builder("cubemap.obj")
                .setName("cubemap")
                .setMaterials(
                        new Material.Builder()
                                .addTextures(
                                        CubemapTextureBuilder.get(name)
                                                .setMipMapping(MinFilter.LINEAR, MagFilter.LINEAR)
                                                .setWrapping(Texture.Wrapping.CLAMP_TO_EDGE)
                                                .noMipMaps()
                                                .build())
                                .build()
                )
                .disableCulling()
                .build());
    }
    
}
