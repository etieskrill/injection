package org.etieskrill.engine.graphics.model

import net.bifreus.kml.AABB
import org.etieskrill.engine.graphics.model.loader.ModelLoaderOptions
import org.etieskrill.engine.graphics.model.loader.loadModel
import org.etieskrill.engine.graphics.texture.TextureCubeMap
import org.etieskrill.engine.graphics.texture.TextureType

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
                AABB(),
                MeshDrawMode.TRIANGLES
            )
        )
    ),
    emptyList(),
    emptyList(),
    AABB()
)
