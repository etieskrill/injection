package org.etieskrill.engine.config;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

import static org.etieskrill.engine.config.ResourcePaths.ENGINE_RESOURCE_PATH;

/**
 * A pretty disgusting workaround to the way SLF4J's SimpleLogger resolves properties:
 * <ol>
 *     <li>System property</li>
 *     <li>Properties file</li>
 *     <li>Defaults</li>
 * </ol>
 * <p>
 * Since user properties from the properties file should override the engine (system) properties, this fallback
 * order does not work, and is manually circumvented in this class.
 * <p>
 * System properties set prior to application start are carried over as usual.
 */
public class SimpleLoggerConfig {

    private static final String ENGINE_CONFIG_FILE = ENGINE_RESOURCE_PATH + "simplelogger.properties";
    private static final String APPLICATION_CONFIG_FILE = "simplelogger.properties";

    static {
        Properties properties = new Properties();
        try {
            //Resources are loaded directly to avoid initialising logger through ResourceReader
            properties.load(ClassLoader.getSystemResourceAsStream(ENGINE_CONFIG_FILE));
        } catch (IOException | IllegalArgumentException | NullPointerException e) {
            //Logging to stdout also to avoid initialisation
            System.out.println("WARN [INJECTION] Failed to load engine logger config\n" + e);
        }

        Properties userProperties = new Properties();
        try {
            userProperties.load(ClassLoader.getSystemResourceAsStream(APPLICATION_CONFIG_FILE));
        } catch (IOException | IllegalArgumentException | NullPointerException e) {
            String reason = e instanceof NullPointerException ?
                    "No user logger config detected" :
                    "User logger config could not be loaded";
            System.out.println("INFO [INJECTION] " + reason + "\n" + e);
        }

        for (String propertyName : properties.stringPropertyNames()) {
            if (System.getProperty(propertyName) != null
                || userProperties.getProperty(propertyName) != null) {
                continue;
            }
            System.setProperty(propertyName, properties.getProperty(propertyName)); //FIXME just ignored if classpath does not contain "simplelogger.properties" file???
        }

        LoggerFactory.getLogger(SimpleLoggerConfig.class).info("Loaded logger config");
    }

    public static void init() {
    }

}
