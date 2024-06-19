package org.etieskrill.orbes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class GameIntegrationTest {

    @Test
    void shouldRunApplication() {
        assertDoesNotThrow(() -> new Game() {
            @Override
            void update(double delta) {
                getWindow().close();
                super.update(delta);
            }
        });
    }

}