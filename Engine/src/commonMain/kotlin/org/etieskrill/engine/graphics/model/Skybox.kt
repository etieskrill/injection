package org.etieskrill.engine.graphics.model

import org.etieskrill.engine.graphics.model.loader.ModelLoaderOptions
import org.etieskrill.engine.graphics.model.loader.loadModel
import org.etieskrill.engine.graphics.texture.TextureCubeMap
import org.etieskrill.engine.graphics.texture.TextureType
import org.joml.primitives.AABBf

class Skybox(
    name: String
) : Model(
    "skybox:$name",
    Node(
        "root",
        meshes = listOf(
            Mesh(
                SkyboxMaterial(name, TextureCubeMap.createFromFile(name, TextureType.DIFFUSE)),
                null,
                loadModel("cubemap.obj", ModelLoaderOptions()).nodes[0].meshes[0].vao,
                AABBf(),
                MeshDrawMode.TRIANGLES
            )
        )
    ),
    emptyList(),
    emptyList(),
    AABBf()
)
