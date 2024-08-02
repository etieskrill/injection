package org.etieskrill.engine.util;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import static java.util.Objects.requireNonNullElse;

public class FileUtils {

    public static @NotNull TypedFile splitTypeFromPath(@NotNull String path) {
        String _path = null;
        String extension = null;
        String fileName;

        int extensionIndex = path.lastIndexOf('.');
        int separatorIndex;
        if ((separatorIndex = path.lastIndexOf(File.separatorChar)) > -1) {}
        else if ((separatorIndex = path.lastIndexOf('/')) > -1) {}
        else if ((separatorIndex = path.lastIndexOf('\\')) > -1) {}

        if (extensionIndex != -1)
            _path = path.substring(0, extensionIndex);
        if (extensionIndex > separatorIndex) {
            extension = path.substring(extensionIndex + 1);
            fileName = path.substring(separatorIndex != -1 ? separatorIndex + 1 : 0, extensionIndex);
        } else {
            fileName = path.substring(separatorIndex != -1 ? separatorIndex + 1 : 0);
        }

        return new TypedFile(
                path,
                requireNonNullElse(_path, path),
                fileName + "." + extension,
                fileName,
                requireNonNullElse(extension, "")
        );
    }

    /**
     * @param fullPath  the entire path including the extension
     * @param path      the path and file name without extension
     * @param name      the name without path including the extension
     * @param fileName  the name without path and without extension
     * @param extension the extension
     */
    public record TypedFile(
            @NotNull String fullPath,
            @NotNull String path,
            @NotNull String name,
            @NotNull String fileName,
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

        public String getFileName() {
            return fileName;
        }

        public String getExtension() {
            return extension;
        }
    }

    private FileUtils() {
        //Not intended for instantiation
    }

}
