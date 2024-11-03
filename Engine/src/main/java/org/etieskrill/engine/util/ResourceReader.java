package org.etieskrill.engine.util;

import lombok.extern.slf4j.Slf4j;
import org.etieskrill.engine.common.ResourceLoadException;
import org.lwjgl.BufferUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.etieskrill.engine.config.ResourcePaths.ENGINE_RESOURCE_PATH;

@Slf4j
public class ResourceReader {

    public static InputStream getClasspathResourceAsStream(String name) {
        return getResourceOrFromEngine(name);
    }

    public static String getClasspathResource(String name) {
        InputStream inputStream = getResourceOrFromEngine(name);

        try (BufferedInputStream bis = new BufferedInputStream(inputStream)) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            for (int result = bis.read(); result != -1; result = bis.read()) {
                buf.write((byte) result);
            }
            inputStream.close();
            return buf.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ByteBuffer getRawClassPathResource(String name) {
        InputStream inputStream = getResourceOrFromEngine(name);

        try {
            byte[] bytes = inputStream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            return buffer.put(bytes).rewind();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static InputStream getResourceOrFromEngine(String name) {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream(name);
        if (inputStream != null) {
            logger.trace("Loading {} from application classpath", name);
            return inputStream;
        }

        inputStream = ClassLoader.getSystemResourceAsStream(ENGINE_RESOURCE_PATH + name);
        if (inputStream == null) {
            throw new ResourceLoadException("Could not load %s from classpath".formatted(name));
        }

        logger.trace("Loading {} from engine classpath", name);
        return inputStream;
    }

    /**
     * Recursively searches the classpath in the specified {@code folder} for any regular files while not listing
     * directories. Links are resolved and followed recursively.
     *
     * @param folder subfolder to search
     * @return all regular files
     */
    public static List<String> getClasspathItems(String folder) {
        var parentResource = ClassLoader.getSystemResource(folder);
        if (parentResource == null) {
            throw new ResourceLoadException("Classpath folder %s could not be located or access was denied".formatted(folder));
        }

        try (var fileSystem = FileSystems.newFileSystem(parentResource.toURI(), Collections.emptyMap())) {
            try (var files = Files.walk(fileSystem.getPath(folder))) {
                return files
                        .filter(Files::isRegularFile)
                        .map(Path::toString)
                        .toList();
            }
        } catch (IOException | URISyntaxException e) {
            throw new ResourceLoadException(e);
        }
    }

}
