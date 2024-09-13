package org.etieskrill.engine.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GLContextConfig {

    //TODO handle thread context transfer

    public static final ThreadLocal<GLContextConfig> CONFIG = ThreadLocal.withInitial(() -> {
        throw new IllegalStateException("Tried to access config for uninitialised GL context");
    });

    private final int _maxTextureUnits;

    public static int getMaxTextureUnits() {
        return CONFIG.get().get_maxTextureUnits();
    }

}
