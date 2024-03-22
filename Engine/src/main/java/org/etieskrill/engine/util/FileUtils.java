package org.etieskrill.engine.util;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import static java.util.Objects.requireNonNullElse;

public class FileUtils {

    public static @NotNull TypedFile splitTypeFromPath(@NotNull String path) {
        String _path = null;
        String extension = null;
        String name;

        int extensionIndex = path.lastIndexOf('.');
        int separatorIndex = path.lastIndexOf(File.separatorChar);

        if (extensionIndex != -1)
            _path = path.substring(0, extensionIndex);
        if (extensionIndex > separatorIndex) {
            extension = path.substring(extensionIndex + 1);
            name = path.substring(separatorIndex != -1 ? separatorIndex + 1 : 0, extensionIndex);
        } else {
            name = path.substring(separatorIndex != -1 ? separatorIndex + 1 : 0);
        }

        return new TypedFile(
                path,
                requireNonNullElse(_path, path),
                name,
                requireNonNullElse(extension, "")
        );
    }

    /**
     * @param fullPath  the entire path including the extension
     * @param path      the path and file name without extension
     * @param name      the name without path and without extension
     * @param extension the extension
     */
    public record TypedFile(
            @NotNull String fullPath,
            @NotNull String path,
            @NotNull String name,
            @NotNull String extension
    ) {
        public String getFullPath() {
            return fullPath;
        }

        public String getPath() {
            return path;
        }

        public String getName() {
            return name;
        }

        public String getExtension() {
            return extension;
        }
    }

}
