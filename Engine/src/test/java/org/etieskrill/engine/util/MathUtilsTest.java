package org.etieskrill.engine.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

class MathUtilsTest {

    @ParameterizedTest
    @MethodSource
    void shouldNormaliseCorrectly(List<Float> components, List<Float> result) {
        assertEquals(result, MathUtils.normalise(components));
    }

    private static Stream<Arguments> shouldNormaliseCorrectly() {
        return Stream.of(
                of(list(), list()),
                of(list(0f), list(0f)),
                of(list(1f), list(1f)),
                of(list(.5f), list(1f)),
                of(list(0f, 1f), list(0f, 1f)),
                of(list(1f, 2f, 2f), list(.2f, .4f, .4f))
        );
    }

    @SafeVarargs
    private static <T> List<T> list(T... values) {
        return new ArrayList<>(List.of(values));
    }
}