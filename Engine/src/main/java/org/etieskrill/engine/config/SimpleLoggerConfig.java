package org.etieskrill.engine.config;

import org.etieskrill.engine.common.ResourceLoadException;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

import static org.etieskrill.engine.config.ResourcePaths.ENGINE_RESOURCE_PATH;
import static org.etieskrill.engine.util.ResourceReader.getClasspathResourceAsStream;

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

    private static final String INJECTION_CONFIG_FILE = ENGINE_RESOURCE_PATH + "injection-simplelogger.properties";
    private static final String USER_CONFIG_FILE = "simplelogger.properties";

    static {
        Properties properties = new Properties();

        try {
            properties.load(getClasspathResourceAsStream(INJECTION_CONFIG_FILE));
        } catch (ResourceLoadException | IOException e) {
            LoggerFactory.getLogger(SimpleLoggerConfig.class).warn("Failed to load engine logger config", e);
        }

        Properties userProperties = new Properties();
        try {
            userProperties.load(getClasspathResourceAsStream(USER_CONFIG_FILE));
        } catch (ResourceLoadException | IOException e) {
            String reason = e instanceof ResourceLoadException ?
                    "No user logger config detected" :
                    "User logger config could not be loaded";
            LoggerFactory.getLogger(SimpleLoggerConfig.class).info(reason, e);
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
