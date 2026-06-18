package org.etieskrill.engine.graphics.model.loader

import org.etieskrill.engine.common.ResourceLoadException
import org.etieskrill.engine.config.MODEL_PATH
import org.etieskrill.engine.util.ResourceReader
import org.lwjgl.assimp.AIFile
import org.lwjgl.assimp.AIFileIO
import org.lwjgl.assimp.AIScene
import org.lwjgl.assimp.Assimp.*
import org.lwjgl.system.MemoryUtil.memAddress
import org.lwjgl.system.MemoryUtil.memCopy
import org.lwjgl.system.MemoryUtil.memUTF8
import kotlin.math.min

private val fileIO: AIFileIO = AIFileIO.create().apply {
    OpenProc { pFileIO, fileName, openMode ->
        val name = memUTF8(fileName)
        val buffer = ResourceReader.getRawResource(MODEL_PATH + name)

        val aiFile = AIFile.create().apply {
            ReadProc { pFile, pBuffer, size, count ->
                val blocksRead = min(buffer.remaining() / size, count)
                memCopy(memAddress(buffer), pBuffer, blocksRead * size)
                buffer.position((buffer.position() + (blocksRead * size)).toInt())
                blocksRead
            }
            SeekProc { pFile, offset, origin ->
                when (origin) {
                    aiOrigin_SET -> buffer.position(offset.toInt())
                    aiOrigin_CUR -> buffer.position(buffer.position() + offset.toInt())
                    aiOrigin_END -> buffer.position(buffer.limit() + offset.toInt())
                }
                0
            }
            FileSizeProc { pFile -> buffer.limit().toLong() }
        }

        aiFile.address()
    }
    CloseProc { pFileIO, pFile ->
        AIFile.create(pFile).apply {
            ReadProc().free()
            SeekProc().free()
            FileSizeProc().free()
        }
        //TODO shouldn't the buffer from OpenProc also be freed here?
    }
}

internal fun importScene(file: String, options: SceneImporterOptions): AIScene {
    //TODO properly set these
    val processFlags: Int =
            aiProcess_Triangulate or
                    aiProcess_CalcTangentSpace or
                    aiProcess_GlobalScale or //FIXME does this even do anything?
//                        aiProcess_SortByPType or
                    if (options.flipTextureCoordinates) aiProcess_FlipUVs else 0 or
//                        aiProcess_OptimizeMeshes or
//                        aiProcess_OptimizeGraph or
                    aiProcess_JoinIdenticalVertices or
//                        aiProcess_GenNormals or
//                        aiProcess_RemoveRedundantMaterials or
//                        aiProcess_FindInvalidData or
//                        aiProcess_GenUVCoords or
//                        aiProcess_TransformUVCoords or
//                        aiProcess_FindInstances or
//                        aiProcess_PreTransformVertices or //TODO add ~static~ flag or smth, but none of this baby shit anymore
                    if (options.flipWinding) aiProcess_FlipWindingOrder else 0

    return aiImportFileEx(file, processFlags, fileIO)
        ?: throw ResourceLoadException("Failed to load scene from '$file' because: ${aiGetErrorString()}")
}

data class SceneImporterOptions(
        val flipTextureCoordinates: Boolean,
        val flipWinding: Boolean
)
