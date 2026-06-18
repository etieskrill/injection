package org.etieskrill.engine.graphics.model;

import org.etieskrill.engine.graphics.texture.Textures;
import org.joml.Vector4f;

public class CubeMapModel extends Model {

    public CubeMapModel(String name) {
        super(new Model.Builder("cubemap.obj")
                .setName("cubemap")
                .setMaterials(new SkyboxMaterial(name, Textures.getSkybox(name), new Vector4f(0.25f), null))
                .setCulling(false)
                .build());
    }

}
