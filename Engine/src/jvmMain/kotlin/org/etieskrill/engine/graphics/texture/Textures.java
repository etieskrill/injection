package org.etieskrill.engine.graphics.texture;

import org.etieskrill.engine.util.EngineTextureLoader;

public class Textures {

    public static CubeMapTexture getSkybox(String name) {
        return (CubeMapTexture) EngineTextureLoader.INSTANCE.load("cubemap/" + name, () ->
                CubeMapTexture.CubemapTextureBuilder.get(name)
                        .setMipMapping(TextureMinFilter.LINEAR, TextureMagFilter.LINEAR)
                        .setWrapping(TextureWrapping.CLAMP_TO_EDGE)
                        .build());
    }

}
