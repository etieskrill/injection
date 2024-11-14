package org.etieskrill.engine.config;

/**
 * Delegates the necessary static config calls before any other part of the application has started.
 * <p>
 * Classes extending {@link org.etieskrill.engine.application.GameApplication GameApplication} call this at the right
 * time on their own, so usually this class never has to be interacted with directly.
 */
public class InjectionConfig {

    static {
        SimpleLoggerConfig.init();
    }

    /**
     * Must be called in a static block before any other static statement, optimally in the application's main class.
     * For example, like so:
     *
     * <pre>
     * ... (only non-static statements)
     * static {
     *     InjectionConfig.init();
     * }
     *
     * private static final Logger logger = LoggerFactory.getLogger(Main.class);
     * ...
     * </pre>
     */
    public static void init() {}

}
