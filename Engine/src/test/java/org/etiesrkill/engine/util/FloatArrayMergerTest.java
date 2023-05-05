package org.etiesrkill.engine.util;

import org.etieskrill.engine.util.FloatArrayMerger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FloatArrayMergerTest {

    private float[] floatArray1, floatArray2, floatArray3, floatArrayRegularResult, floatArrayTripleResult;

    @BeforeEach
    public void setup() {
        floatArray1 = new float[] {0f, 1f, 2f, 3f, 4f, 5f};
        floatArray2 = new float[] {2f, 3f, 4f, 5f};
        floatArray3 = new float[] {6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f};
        floatArrayRegularResult = new float[] {0f, 1f, 2f, 2f, 3f, 3f, 4f, 5f, 4f, 5f};
        floatArrayTripleResult = new float[] {0f, 1f, 2f, 2f, 3f, 6f, 7f, 8f, 9f, 3f, 4f, 5f, 4f, 5f, 10f, 11f, 12f, 13f};
    }

    @Test
    public void testArrayMergeRegular() {
        assertArrayEquals(floatArrayRegularResult, FloatArrayMerger.merge(floatArray1, floatArray2, 3, 2));
    }

    @Test
    public void testArrayMergeTriple() {
        float[] floatArrayTripleIntermediateResult = FloatArrayMerger.merge(floatArray1, floatArray2, 3, 2);
        float[] floatArrayTripleActualResult = FloatArrayMerger.merge(floatArrayTripleIntermediateResult, floatArray3, 5, 4);

        assertArrayEquals(floatArrayTripleResult, floatArrayTripleActualResult);
    }

}
