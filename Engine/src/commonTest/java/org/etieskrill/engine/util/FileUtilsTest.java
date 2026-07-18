package org.etieskrill.engine.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class FileUtilsTest {

    @MethodSource
    @ParameterizedTest
    void splitTypeFromPath(String path, FileUtils.TypedFile expected) {
        assertEquals(expected, FileUtils.splitTypeFromPath(path));
    }

    static Stream<Arguments> splitTypeFromPath() {
        return Stream.of(
                arguments("folder/file.ext", new FileUtils.TypedFile("folder/file.ext", "folder", "folder/file", "file.ext", "file", "", "ext")),
                arguments("/parent/child/file.ext", new FileUtils.TypedFile("/parent/child/file.ext", "/parent/child", "/parent/child/file", "file.ext", "file", "", "ext")),
                arguments("file.ext", new FileUtils.TypedFile("file.ext", "", "file", "file", "file.ext", "", "ext")),
                arguments("file", new FileUtils.TypedFile("file", "", "", "file", "file", "", "")),
                arguments("file.subExt.ext", new FileUtils.TypedFile("file.subExt.ext", "", "file", "file", "file.subExt.ext", "subExt", "ext")),
                arguments("file.subExt1.subExt2.ext", new FileUtils.TypedFile("file.subExt1.subExt2.ext", "", "file", "file", "file.subExt1.subExt2.ext", "subExt1.subExt2", "ext"))
        );
    }

}