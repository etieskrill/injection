package org.etieskrill.game.animeshun;

import org.etieskrill.engine.window.Window;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class GameIntegrationTest {

    @Test
    void shouldRunApplication() {
        assertDoesNotThrow(() -> new Game() {
            @Override
            void interrupt(Window window) {
                window.close();
            }
        });
    }

}