package org.etieskrill.engine.util;

import org.etieskrill.engine.common.ResourceLoadException;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.etieskrill.engine.config.ResourcePaths.ENGINE_RESOURCE_PATH;

//TODO rework finally for external resources, use classpath as fallback
//TODO probs meant by ^, but REMOVE "prefixes"
public class ResourceReader {

    private static final Logger logger = LoggerFactory.getLogger(ResourceReader.class);

    public static InputStream getClasspathResourceAsStream(String name) {
        return getResourceOrFromEngine(name, true, true);
    }

    public static InputStream getClasspathResourceAsStream(String name, boolean includeApplication, boolean includeEngine) {
        return getResourceOrFromEngine(name, includeApplication, includeEngine);
    }

    public static String getClasspathResource(String name) {
        InputStream inputStream = getResourceOrFromEngine(name, true, true);

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

    public static ByteBuffer getRawClasspathResource(String name) {
        InputStream inputStream = getResourceOrFromEngine(name, true, true);

        try {
            byte[] bytes = inputStream.readAllBytes();
            inputStream.close();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            return buffer.put(bytes).rewind();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static InputStream getResourceOrFromEngine(String name, boolean includeApplication, boolean includeEngine) {
        InputStream inputStream;

        if (includeApplication && (inputStream = ClassLoader.getSystemResourceAsStream(name)) != null) {
            logger.trace("Loading {} from application classpath", name);
            return inputStream;
        }

        if (includeEngine && (inputStream = ClassLoader.getSystemResourceAsStream(ENGINE_RESOURCE_PATH + name)) != null) {
            logger.trace("Loading {} from engine classpath", name);
            return inputStream;
        }

        throw new ResourceLoadException("Could not load %s from classpath".formatted(name));
    }

    /**
     * Recursively searches the classpath in the specified {@code folder} for any regular files while not listing
     * directories. Links are resolved and followed recursively.
     *
     * @param folder subfolder to search
     * @return all regular files
     */
    public static List<String> getClasspathItems(String folder) {
        var parentResource = getResourcePath(folder);
        if (parentResource == null) {
            throw new ResourceLoadException("Classpath folder %s could not be located or access was denied".formatted(folder));
        }

        return Stream.of(getClasspathResource(parentResource).split("\n"))
                .map(file -> parentResource + "/" + file)
                .toList();
    }

    public static boolean classpathResourceExists(String file) {
        return null != ClassLoader.getSystemResource(file)
               || null != ClassLoader.getSystemResource(ENGINE_RESOURCE_PATH + file);
    }

    public static @Nullable String getResourcePath(String file) {
        var appResource = ClassLoader.getSystemResource(file);
        if (appResource != null) return file;

        var engineResource = ClassLoader.getSystemResource(ENGINE_RESOURCE_PATH + file);
        if (engineResource != null) return ENGINE_RESOURCE_PATH + file;

        return null;
    }

}
