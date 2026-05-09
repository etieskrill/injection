package org.etieskrill.engine.config

/**
 * Delegates the necessary static config calls before any other part of the application has started.
 *
 * Classes extending [App][org.etieskrill.engine.application.App] call this at the right
 * time on their own, so usually this class never has to be interacted with directly.
 */
object InjectionConfig {

    init {
        SimpleLoggerConfig.init()
    }

    /**
     * Must be called in a statically evaluated block before any other static statement, optimally in the application
     * main class's companion object, or in the first init block if the application is an `object`. For example, like so:
     *
     * ```
     * ...
     * companion object {
     *     init {
     *         InjectionConfig.init();
     *     }
     * }
     * ...
     * ```
     */
    fun init() = Unit

}
