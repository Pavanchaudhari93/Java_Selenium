package com.framework.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Thread-safe central configuration reader.
 * <p>
 * Loads {@code config/config.properties} as a base, overlays the environment
 * specific file {@code config/config-<env>.properties} (selected via the
 * {@code -Denv=...} system property) and finally gives highest precedence to
 * JVM system properties. Loading is synchronized and happens exactly once.
 *
 * <pre>
 * Precedence (highest first):
 *   1. System properties (-Dbrowser=firefox)
 *   2. config/config-&lt;env&gt;.properties
 *   3. config/config.properties
 * </pre>
 */
public final class ConfigReader {

    private static final Logger LOG = LogManager.getLogger(ConfigReader.class);

    private static final Properties PROPERTIES = new Properties();
    private static volatile boolean loaded = false;

    private ConfigReader() {
        // utility class - no instantiation
    }

    /**
     * Loads the configuration files exactly once (thread-safe).
     * Idempotent; subsequent calls are no-ops.
     */
    public static synchronized void loadConfig() {
        if (loaded) {
            return;
        }
        loadFile("config/config.properties", true);
        String env = System.getProperty("env", PROPERTIES.getProperty("env", "qa"));
        loadFile("config/config-" + env + ".properties", false);
        LOG.info("Configuration loaded for environment: [{}]", env);
        loaded = true;
    }

    /**
     * Reads a property applying the documented precedence.
     *
     * @param key property key
     * @return value or {@code null} when not found
     */
    public static String get(String key) {
        loadConfig();
        String systemValue = System.getProperty(key);
        return systemValue != null ? systemValue : PROPERTIES.getProperty(key);
    }

    /**
     * Reads a property with a fallback default value.
     *
     * @param key          property key
     * @param defaultValue fallback when the key is absent
     * @return value or default
     */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Reads an int property.
     *
     * @param key          property key
     * @param defaultValue fallback on missing or unparseable value
     * @return parsed int
     */
    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LOG.warn("Property [{}] value [{}] is not a valid int, using default [{}]", key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Reads a boolean property.
     *
     * @param key          property key
     * @param defaultValue fallback on missing or unparseable value
     * @return parsed boolean
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * @return currently active environment (dev/qa/staging/prod)
     */
    public static String getEnv() {
        return System.getProperty("env", get("env", "qa"));
    }

    private static void loadFile(String resource, boolean required) {
        try (InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                if (required) {
                    throw new IllegalStateException("Required config resource not found on classpath: " + resource);
                }
                LOG.warn("Optional config resource not found on classpath: {}", resource);
                return;
            }
            PROPERTIES.load(in);
            LOG.debug("Loaded config resource: {}", resource);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config resource: " + resource, e);
        }
    }
}
