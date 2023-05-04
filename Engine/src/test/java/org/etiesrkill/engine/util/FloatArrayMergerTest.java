package org.etiesrkill.engine.util;

import org.etieskrill.engine.util.FloatArrayMerger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FloatArrayMergerTest {

    private float[] floatArray1, floatArray2, floatArrayResult;

    @BeforeEach
    public void setup() {
        floatArray1 = new float[]{0f, 1f, 2f, 3f, 4f, 5f};
        floatArray2 = new float[]{2f, 3f, 4f, 5f};
        floatArrayResult = new float[]{0f, 1f, 2f, 2f, 3f, 3f, 4f, 5f, 4f, 5f};
    }

    @Test
    public void testArrayMergeRegular() {
        assertArrayEquals(floatArrayResult, FloatArrayMerger.merge(floatArray1, floatArray2, 3, 2));
    }

}
