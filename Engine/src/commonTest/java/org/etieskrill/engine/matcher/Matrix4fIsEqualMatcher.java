package org.etieskrill.engine.matcher;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.joml.Matrix4fc;

import static java.util.Objects.requireNonNull;

public class Matrix4fIsEqualMatcher extends TypeSafeMatcher<Matrix4fc> {

    private static final float DEFAULT_EPSILON = .000001f;

    private final Matrix4fc matrix;
    private final float epsilon;

    private Matrix4fIsEqualMatcher(Matrix4fc matrix, float epsilon) {
        this.matrix = matrix;
        this.epsilon = epsilon;
    }

    public static Matcher<Matrix4fc> matrixEqualTo(Matrix4fc matrix) {
        return new Matrix4fIsEqualMatcher(matrix, DEFAULT_EPSILON);
    }

    public static Matcher<Matrix4fc> matrixEqualTo(Matrix4fc matrix, float epsilon) {
        if (epsilon < 0) throw new IllegalArgumentException("Epsilon must be non-negative");
        return new Matrix4fIsEqualMatcher(requireNonNull(matrix), epsilon);
    }

    @Override
    protected boolean matchesSafely(Matrix4fc item) {
        return matrix.equals(item, epsilon);
    }

    @Override
    public void describeTo(Description description) {
        description
                .appendText("equals +/- ")
                .appendValue(epsilon)
                .appendText(" for each component: ")
                .appendValue(matrix)
        ;
    }

}
