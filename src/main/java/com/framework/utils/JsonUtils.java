package com.framework.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * JSON serialization/deserialization helpers built on Jackson.
 * <p>
 * Provides POJO &lt;-&gt; JSON conversion and file loading. The shared
 * {@link ObjectMapper} is thread-safe after configuration, so a single
 * static instance is used.
 */
public final class JsonUtils {

    private static final Logger LOG = LogManager.getLogger(JsonUtils.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private JsonUtils() {
        // utility class - no instantiation
    }

    /**
     * Serializes an object to a JSON string.
     *
     * @param object object to serialize
     * @return JSON string
     */
    public static String toJson(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * Deserializes a JSON string into an object of the given type.
     *
     * @param json  JSON string
     * @param clazz target type
     * @param <T>   target type
     * @return deserialized object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize JSON to " + clazz.getName(), e);
        }
    }

    /**
     * Reads a JSON file and maps it to an object (or array) of the given type.
     *
     * @param filePath path to the JSON file
     * @param clazz    target type, e.g. {@code User[].class}
     * @param <T>      target type
     * @return deserialized object
     */
    public static <T> T readJsonFile(String filePath, Class<T> clazz) {
        try {
            return MAPPER.readValue(new File(filePath), clazz);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read JSON file: " + filePath, e);
        }
    }

    /**
     * Reads a JSON file into a generic {@link Map}.
     *
     * @param filePath path to the JSON file
     * @return map representation
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> readJsonAsMap(String filePath) {
        try {
            return MAPPER.readValue(new File(filePath), Map.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read JSON file: " + filePath, e);
        }
    }

    /**
     * Parses a JSON string into a traversable {@link JsonNode}.
     *
     * @param json JSON string
     * @return root node
     */
    public static JsonNode readTree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse JSON tree", e);
        }
    }

    /**
     * @return the shared, thread-safe ObjectMapper.
     */
    public static ObjectMapper getObjectMapper() {
        return MAPPER;
    }
}
