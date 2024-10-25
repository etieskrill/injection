package org.etieskrill.engine.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import static java.util.Objects.requireNonNullElse;

public class FileUtils {

    public static @NotNull TypedFile splitTypeFromPath(@NotNull String path) {
//        if (!isRegularFile(Path.of(path))) {
//            throw new IllegalArgumentException(path + " is not a file");
//        }

        return new TypedFile(path);
    }

    @Data
    @AllArgsConstructor
    public static class TypedFile {
        /**
         * The entire path including the extension.
         */
        @NotNull String fullPath;
        @NotNull String path;
        /**
         * Path and file name without extension.
         */
        @NotNull String pathName;
        /**
         * Name without path including the extension.
         */
        @NotNull String name;
        /**
         * Name without path and without extension.
         */
        @NotNull String fileName;
        @NotNull String subExtension;
        @NotNull String extension;

        public TypedFile(@NotNull String path) {
            String _path = null;
            String extension = null;
            String fileName;

            int extensionIndex = path.lastIndexOf('.');
            int separatorIndex;
            if ((separatorIndex = path.lastIndexOf(File.separatorChar)) > -1) {
            } else if ((separatorIndex = path.lastIndexOf('/')) > -1) {
            } else if ((separatorIndex = path.lastIndexOf('\\')) > -1) {
            }

            if (extensionIndex != -1)
                _path = path.substring(0, extensionIndex);
            if (extensionIndex > separatorIndex) {
                extension = path.substring(extensionIndex + 1);
                fileName = path.substring(separatorIndex != -1 ? separatorIndex + 1 : 0, extensionIndex);
            } else {
                fileName = path.substring(separatorIndex != -1 ? separatorIndex + 1 : 0);
            }

            String onlyPath = path.lastIndexOf('/') == -1 ? path : path.substring(0, path.lastIndexOf('/'));

            String subExtension = null;
            int subExtensionIndex = fileName.lastIndexOf('.');
            if (subExtensionIndex != -1) {
                subExtension = fileName.substring(subExtensionIndex + 1);
            }

            this.fullPath = path;
            this.path = onlyPath;
            this.pathName = requireNonNullElse(_path, path);
            this.name = fileName + "." + extension;
            this.fileName = fileName;
            this.subExtension = requireNonNullElse(subExtension, "");
            this.extension = requireNonNullElse(extension, "");
        }
    }

    private FileUtils() {
        //Not intended for instantiation
    }

}
