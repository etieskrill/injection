package org.etieskrill.engine.graphics.model;

import org.etieskrill.engine.graphics.texture.Textures;
import org.joml.Vector4f;

public class CubeMapModel extends Model {

    public CubeMapModel(String name) {
        super(createModel(name));
    }

    private static Model createModel(String name) {
        var builder = new Model.Builder("cubemap.obj");
        builder.setName("cubemap");
        builder.setMaterials(new SkyboxMaterial(name, Textures.getSkybox(name), new Vector4f(0.25f), null));
        builder.setCulling(false);
        return builder.build();
    }

}
