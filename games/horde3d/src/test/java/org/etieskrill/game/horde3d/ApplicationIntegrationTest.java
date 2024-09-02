package org.etieskrill.game.horde3d;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ApplicationIntegrationTest {

    @Test
    void shouldRunApplication() {
        assertDoesNotThrow(() -> new Application() {
            @Override
            protected void _loop() {
                doLoop();
            }
        });
    }

}
