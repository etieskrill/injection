package org.etieskrill.engine.entity.component;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.ReflectionUtils;

import java.util.stream.Stream;

import static org.etieskrill.engine.matcher.Matrix4fIsEqualMatcher.matrixEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.joml.Math.toRadians;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

class TransformTest {

    private static final Matrix4fc IDENTITY = new Matrix4f();

    Transform fixture;

    @BeforeEach
    void setUp() {
        fixture = new Transform();
    }

    @Test
    void emptyConstructor_IsIdentity_AndNotDirty() {
        assertThat(fixture.getMatrix(), is(IDENTITY));
        assertDirtied(false);
    }

    @ParameterizedTest
    @MethodSource
    void customConstructor_GivesCorrectMatrix(Transform transform, Matrix4f result) {
        fixture.set(transform);
        assertThat(fixture.getMatrix(), matrixEqualTo(result));
    }

    @ParameterizedTest
    @MethodSource("customConstructor_GivesCorrectMatrix")
    void deconstructedTransform_IsCorrect(Transform result, Matrix4fc matrix) {
        fixture = Transform.fromMatrix4f(matrix);
        assertThat(fixture, is(result));
    }

    static Stream<Arguments> customConstructor_GivesCorrectMatrix() {
        return Stream.of(
                arguments(new Transform(), IDENTITY),
                arguments(new Transform().setScale(0), IDENTITY.scale(0, new Matrix4f())),
                arguments(new Transform().setPosition(new Vector3f(1, 2, 3)),
                        IDENTITY.translate(1, 2, 3, new Matrix4f())),
                arguments(new Transform().applyRotation(quat ->
                                quat.rotateX(toRadians(90)).rotateY(toRadians(-90))),
                        new Matrix4f().rotateX(toRadians(90)).rotateY(toRadians(-90))),
                arguments(new Transform().applyScale(scale ->
                                scale.set(4, 5, 6)),
                        IDENTITY.scale(4, 5, 6, new Matrix4f())),
                arguments(new Transform()
                                .setPosition(new Vector3f(1, 2, 3))
                                .applyRotation(quat -> quat.rotateX(toRadians(90)).rotateY(toRadians(-90)))
                                .setScale(new Vector3f(4, 5, 6)),
                        new Matrix4f() //According to the documentation, this is the correct way of specifying the transformation of T * R * S, even though, logically speaking, it should now be the wrong way around
                                .translate(1, 2, 3)
                                .rotateX(toRadians(90)).rotateY(toRadians(-90))
                                .scale(4, 5, 6)),
                arguments(new Transform()
                                .setScale(new Vector3f(4, 5, 6))
                                .applyRotation(quat -> quat.rotateX(toRadians(90)).rotateY(toRadians(-90)))
                                .setPosition(new Vector3f(1, 2, 3)),
                        new Matrix4f() //According to the documentation, this is the correct way of specifying the transformation of T * R * S, even though, logically speaking, it should now be the wrong way around
                                .translate(1, 2, 3)
                                .rotateX(toRadians(90)).rotateY(toRadians(-90))
                                .scale(4, 5, 6)),
                arguments(new Transform()
                                .applyRotation(quat -> quat.rotateX(toRadians(90)))
                                .setScale(new Vector3f(4, 5, 6)),
                        new Matrix4f() //According to the documentation, this is the correct way of specifying the transformation of T * R * S, even though, logically speaking, it should now be the wrong way around
                                .rotateX(toRadians(90))
                                .scale(4, 5, 6)),
                arguments(new Transform()
                                .setPosition(new Vector3f(1, 2, 3))
                                .applyRotation(quat -> quat.rotateX(toRadians(90)).rotateY(toRadians(-90))),
                        new Matrix4f() //According to the documentation, this is the correct way of specifying the transformation of T * R * S, even though, logically speaking, it should now be the wrong way around
                                .translate(1, 2, 3)
                                .rotateX(toRadians(90)).rotateY(toRadians(-90)))
        );
    }

    @Test
    void assertSetPosition_FailsOnNull_AndPassesByValue_AndDirtiesTransform() {
        assertThrows(NullPointerException.class, () -> fixture.setPosition((Vector3f) null));

        Vector3f position = new Vector3f(1, 2, 3);
        fixture.setPosition(position);
        assertDirtied(true);

        position.set(4, 5, 6);
        assertThat("Position must not be passed by reference", fixture.getPosition(), is(new Vector3f(1, 2, 3)));
    }

    @Test
    void assertTranslate_FailsOnNull_AndPassesByValue_AndDirtiesTransform() {
        assertThrows(NullPointerException.class, () -> fixture.translate((Vector3f) null));

        fixture = new Transform(new Vector3f(1, 2, 3), new Quaternionf(), new Vector3f(1));
        assertDirtied(false);

        Vector3f translation = new Vector3f(1, 2, 3);
        fixture.translate(translation);
        assertDirtied(true);

        translation.set(4, 5, 6);
        assertThat(fixture.getPosition(), is(new Vector3f(2, 4, 6)));
    }

    @Test
    void setScale() {
        //TODO copy paste from above
    }

    @Test
    void applyScale() {
    }

    @Test
    void setRotation() {
    }

    @Test
    void applyRotation() {
    }

    @Test
    void set() {
    }

    @ParameterizedTest
    @MethodSource
    void apply_CorrectlyTransforms(Transform firstTransform, Transform secondTransform, Matrix4f result) {
        assertThat(firstTransform.apply(secondTransform).getMatrix(), matrixEqualTo(result));
    }

    @ParameterizedTest
    @MethodSource("apply_CorrectlyTransforms")
    void applyToTarget_CorrectlyTransforms(Transform firstTransform, Transform secondTransform, Matrix4f result) {
        assertThat(firstTransform.apply(secondTransform, new Transform()).getMatrix(), matrixEqualTo(result));
    }

    private static Stream<Arguments> apply_CorrectlyTransforms() {
        return Stream.of(
                arguments(new Transform(), new Transform(), IDENTITY),
                arguments(
                        new Transform(),
                        new Transform().translate(new Vector3f(1, 2, 3)),
                        new Matrix4f().translate(1, 2, 3)),
                arguments(
                        new Transform().translate(new Vector3f(1, 2, 3)),
                        new Transform(),
                        new Matrix4f().translate(1, 2, 3)),
                arguments(
                        new Transform().translate(new Vector3f(1, 2, 3)),
                        new Transform().translate(new Vector3f(1, 2, 3)),
                        new Matrix4f().translate(2, 4, 6)),
                arguments(
                        new Transform(),
                        new Transform().applyScale(scale -> scale.mul(2)),
                        new Matrix4f().scale(2)),
                arguments(
                        new Transform().applyScale(scale -> scale.mul(2)),
                        new Transform(),
                        new Matrix4f().scale(2)),
                arguments(
                        new Transform().applyScale(scale -> scale.mul(2)),
                        new Transform().applyScale(scale -> scale.mul(2)),
                        new Matrix4f().scale(4)),
                arguments(
                        new Transform(),
                        new Transform().applyRotation(quat -> quat.rotateX(toRadians(90)).rotateY(toRadians(-90))),
                        new Matrix4f().rotateX(toRadians(90)).rotateY(toRadians(-90))),
                arguments(
                        new Transform().applyRotation(quat -> quat.rotateX(toRadians(90)).rotateY(toRadians(-90))),
                        new Transform(),
                        new Matrix4f().rotateX(toRadians(90)).rotateY(toRadians(-90))),
                arguments(
                        new Transform().applyRotation(quat -> quat.rotateX(toRadians(90)).rotateY(toRadians(-90))),
                        new Transform().applyRotation(quat -> quat.rotateX(toRadians(90)).rotateY(toRadians(-90))),
                        new Matrix4f().rotateX(toRadians(90)).rotateY(toRadians(-90)).rotateX(toRadians(90)).rotateY(toRadians(-90))),
                arguments(
                        new Transform().applyScale(scale -> scale.mul(2)).applyRotation(quat -> quat.rotateX(toRadians(90)).rotateY(toRadians(-90))).translate(new Vector3f(1, 2, 3)),
                        new Transform(),
                        new Matrix4f().translate(1, 2, 3).rotateX(toRadians(90)).rotateY(toRadians(-90)).scale(2)),
                arguments(
                        new Transform(),
                        new Transform().applyScale(scale -> scale.mul(2)).applyRotation(quat -> quat.rotateX(toRadians(90)).rotateY(toRadians(-90))).translate(new Vector3f(1, 2, 3)),
                        new Matrix4f().translate(1, 2, 3).rotateX(toRadians(90)).rotateY(toRadians(-90)).scale(2)),
                arguments(
                        new Transform().applyScale(scale -> scale.mul(2)).applyRotation(quat -> quat.rotateX(toRadians(90)).rotateY(toRadians(-90))).translate(new Vector3f(1, 2, 3)),
                        new Transform().applyScale(scale -> scale.mul(2)).applyRotation(quat -> quat.rotateX(toRadians(90)).rotateY(toRadians(-90))).translate(new Vector3f(2, 3, 4)),
                        new Matrix4f().translate(1, 2, 3).rotateX(toRadians(90)).rotateY(toRadians(-90)).scale(2).translate(2, 3, 4).rotateX(toRadians(90)).rotateY(toRadians(-90)).scale(2))
        );
    }

    @Test
    void toMat_UpdatesMatrix_OnlyWhenDirty() throws NoSuchMethodException {
        fixture = spy(fixture);
        fixture.getMatrix();

        verify(fixture, never()).updateTransform();

        fixture.setPosition(new Vector3f(1, 2, 3));
        assertDirtied(true);
        Matrix4fc matrix = fixture.getMatrix();
        verify(fixture, times(1)).updateTransform();

        assertThat(matrix, matrixEqualTo(new Matrix4f().translate(1, 2, 3)));
    }

    private void assertDirtied(boolean shouldBeDirty) {
        ReflectionUtils.tryToReadFieldValue(Transform.class, "dirty", fixture)
                .ifSuccess(dirty -> assertThat("Transform must" + (shouldBeDirty ? " " : " not ") + "be dirtied", dirty, is(shouldBeDirty)))
                .ifFailure(Assertions::fail);
    }

}
