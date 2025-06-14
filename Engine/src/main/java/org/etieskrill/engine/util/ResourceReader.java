package org.etieskrill.engine.util;

import org.etieskrill.engine.common.ResourceLoadException;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.etieskrill.engine.config.ResourcePaths.ENGINE_RESOURCE_PATH;

//TODO remove "prefixes", add scene base path local resources
public class ResourceReader {

    private static final Logger logger = LoggerFactory.getLogger(ResourceReader.class);

    public static InputStream getResourceAsStream(String name) {
        return resolveResource(name, true, true, true);
    }

    public static String getResource(String name) {
        InputStream inputStream = resolveResource(name, true, true, true);

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

    public static ByteBuffer getRawResource(String name) {
        InputStream inputStream = resolveResource(name, true, true, true);

        try {
            byte[] bytes = inputStream.readAllBytes();
            inputStream.close();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            return buffer.put(bytes).rewind();
        } catch (IOException e) {
            throw new ResourceLoadException(e);
        }
    }

    private static InputStream resolveResource(
            String name,
            boolean includeExternal, boolean includeApplication, boolean includeEngine
    ) {
        InputStream inputStream;

        if (includeExternal) { //TODO allow only project specific resources
            try {
                inputStream = new FileInputStream(name);
                logger.trace("Loading {} from external path", name);
                return inputStream;
            } catch (FileNotFoundException ignored) {
            }
        }

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
     * Searches the classpath in the specified {@code directory} for any regular files while not listing
     * directories. Subdirectories are <b>NOT</b> searched. Links are resolved.
     *
     * @param directory directory to search
     * @return all regular files
     */
    public static List<String> getClasspathItems(String directory) {
        var parentResource = getResourcePath(directory);
        if (parentResource == null) {
            throw new ResourceLoadException("Classpath directory %s could not be located or access was denied".formatted(directory));
        }

        URI realPath;
        try {
            realPath = ClassLoader.getSystemResource(parentResource).toURI();
        } catch (URISyntaxException e) {
            throw new ResourceLoadException(e);
        }

        //kinda hacky, dunno how reliable this is
        switch (realPath.getScheme()) {
            case "file" -> { //unarchived dev build
                return listFilePaths(Path.of(realPath));
            }
            case "jar" -> { //"normal" packaged build
                try (var fileSystem = FileSystems.newFileSystem(realPath, Collections.emptyMap())) {
                    var a = listFilePaths(fileSystem.getPath(parentResource));
                    System.out.println(a);
                    return a;
                } catch (IOException e) {
                    throw new ResourceLoadException("Failed to open jar file system", e);
                }
            }
            default -> throw new ResourceLoadException("Unsupported resource scheme '" + realPath.getScheme() + "'");
        }
    }

    private static List<String> listFilePaths(Path path) {
        try (var files = Files.walk(path)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .toList();
        } catch (IOException e) {
            throw new ResourceLoadException(e);
        }
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
