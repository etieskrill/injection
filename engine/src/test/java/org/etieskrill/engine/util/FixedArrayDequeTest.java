package org.etieskrill.engine.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class FixedArrayDequeTest {

    FixedArrayDeque<Object> fixture;

    static final int NUM_ELEMENTS = 10;

    @BeforeEach
    void setUp() {
        fixture = new FixedArrayDeque<>(NUM_ELEMENTS);
    }

    @Test
    void shouldNotExceedSpecifiedSize() {
        assertThat(fixture.size(), is(0));

        fillQueue();
        assertThat(fixture.size(), is(10));

        fixture.add(new Object());
        assertThat(fixture.size(), is(10));
    }

    @Test
    void shouldOverrideHead() {
        fillQueue();

        Object firstElement = fixture.getLast();
        Object nextElement = new Object();

        fixture.push(nextElement);

        assertThat(firstElement, is(not(nextElement))); //more of a sanity check
        assertThat(fixture.getFirst(), is(nextElement));
        assertThat(firstElement, not(in(fixture)));
    }

    @Test
    void shouldOverrideTail() {
        fillQueue();

        Object lastElement = fixture.getFirst();
        Object nextElement = new Object();

        fixture.addLast(nextElement);

        assertThat(lastElement, is(not(nextElement)));
        assertThat(fixture.getLast(), is(nextElement));
        assertThat(lastElement, not(in(fixture)));
    }

    @Test
    void shouldAddAll() {
        fixture.addAll(List.of(
                new Object(),
                new Object(),
                new Object(),
                new Object(),
                new Object()
        ));

        assertThat(fixture.size(), is(5));
    }

    @Test
    void shouldLoopOnAddAllOverflow() {
        List<Object> objects = new ArrayList<>();
        for (int i = 0; i < 2 * NUM_ELEMENTS; i++) {
            objects.add(new Object());
        }

        fixture.addAll(objects);

        assertThat(fixture.size(), is(10));
        assertThat(fixture, containsInAnyOrder(objects.subList(10, 20).toArray()));
        assertThat(fixture, not(containsInAnyOrder(objects.subList(0, 10).toArray())));
    }

    private void fillQueue() {
        for (int i = 0; i < NUM_ELEMENTS; i++) {
            fixture.push(new Object());
        }
    }
}