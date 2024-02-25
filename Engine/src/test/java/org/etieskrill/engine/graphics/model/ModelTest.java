package org.etieskrill.engine.graphics.model;

import org.etieskrill.engine.graphics.animation.Animation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelTest {

    static Model ANIMATED_MODEL;

    @BeforeAll
    static void setup() {
        ANIMATED_MODEL = Model.ofFile("vampire_hip_hop.fbx");
    }

    @Test
    void test() {
        assertEquals(1, ANIMATED_MODEL.getAnimations().size());

        Animation animation = ANIMATED_MODEL.getAnimations().getFirst();

        System.out.println(animation);
    }

}