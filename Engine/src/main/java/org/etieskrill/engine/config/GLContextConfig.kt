package org.etieskrill.engine.config;

data class GLContextConfig(
    val maxTextureUnits: Int
) {

    //TODO handle thread context transfer

    companion object {
        val CONFIG: ThreadLocal<GLContextConfig> = ThreadLocal.withInitial {
            throw IllegalStateException("Tried to access config for uninitialised GL context")
        }

        val MAX_TEXTURE_UNITS: Int get() = CONFIG.get().maxTextureUnits
    }

}
