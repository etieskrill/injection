package org.etieskrill.engine.graphics.model.loader;

import org.etieskrill.engine.common.ResourceLoadException;
import org.etieskrill.engine.util.ResourceReader;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.assimp.AIFile;
import org.lwjgl.assimp.AIFileIO;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

import static org.etieskrill.engine.config.ResourcePaths.MODEL_PATH;
import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memCopy;

class Importer {

    private static final Supplier<AIFileIO> fileIO = () -> AIFileIO.create().OpenProc((pFileIO, fileName, openMode) -> {
        String name = MemoryUtil.memUTF8(fileName);
        ByteBuffer buffer = ResourceReader.getRawClassPathResource(MODEL_PATH + name);

        AIFile aiFile = AIFile.create().ReadProc((pFile, pBuffer, size, count) -> {
            long blocksRead = Math.min(buffer.remaining() / size, count);
            memCopy(memAddress(buffer), pBuffer, blocksRead * size);
            buffer.position((int) (buffer.position() + (blocksRead * size)));
            return blocksRead;
        }).SeekProc((pFile, offset, origin) -> {
            switch (origin) {
                case aiOrigin_SET -> buffer.position((int) offset);
                case aiOrigin_CUR -> buffer.position(buffer.position() + (int) offset);
                case aiOrigin_END -> buffer.position(buffer.limit() + (int) offset);
            }
            return 0;
        }).FileSizeProc(pFile -> buffer.limit());

        return aiFile.address();
    }).CloseProc((pFileIO, pFile) -> {
        AIFile aiFile = AIFile.create(pFile);
        aiFile.ReadProc().free();
        aiFile.SeekProc().free();
        aiFile.FileSizeProc().free();
        //TODO shouldn't the buffer from OpenProc also be freed here?
    });

    static @NotNull AIScene importScene(String file, Options options) {
        //TODO properly set these
        int processFlags =
                aiProcess_Triangulate |
                        aiProcess_CalcTangentSpace |
//                        aiProcess_SortByPType |
                        (options.flipTextureCoordinates() ? aiProcess_FlipUVs : 0) |
//                        aiProcess_OptimizeMeshes |
//                        aiProcess_OptimizeGraph |
//                        aiProcess_JoinIdenticalVertices |
//                        aiProcess_RemoveRedundantMaterials |
//                        aiProcess_FindInvalidData |
//                        aiProcess_GenUVCoords |
//                        aiProcess_TransformUVCoords |
//                        aiProcess_FindInstances |
//                        aiProcess_PreTransformVertices | //TODO add ~static~ flag or smth, but none of this baby shit anymore
                        (options.flipWinding() ? aiProcess_FlipWindingOrder : 0);

        AIScene aiScene = aiImportFileEx(file, processFlags, fileIO.get()); //TODO i think this needs a new fileIO instance per import? still check tho
        if (aiScene == null)
            throw new ResourceLoadException("Failed to load scene from '" + file + "' because: " + aiGetErrorString());
        return aiScene;
    }

    record Options(
            boolean flipTextureCoordinates,
            boolean flipWinding
    ) {
    }

}
