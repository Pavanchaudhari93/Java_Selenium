package com.framework.api;

import io.qameta.allure.Step;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.util.Map;

/**
 * Response validation helpers: status codes, JSON schema validation,
 * field-level assertions and response-time checks.
 * <p>
 * These are hard assertions (they fail the test immediately on mismatch).
 * Use {@link com.framework.assertions.SoftAssertHelper} for multi-field soft
 * validation.
 */
public final class ApiResponseValidator {

    private static final Logger LOG = LogManager.getLogger(ApiResponseValidator.class);

    private ApiResponseValidator() {
        // utility class - no instantiation
    }

    /**
     * Asserts the response status code.
     *
     * @param response   the response
     * @param expected   expected status code
     */
    @Step("Validate status code {expected}")
    public static void validateStatusCode(Response response, int expected) {
        int actual = response.getStatusCode();
        LOG.info("Validating status code | expected=[{}] actual=[{}]", expected, actual);
        Assert.assertEquals(actual, expected,
                "Unexpected status code. Body: " + response.getBody().asString());
    }

    /**
     * Validates the response body against a JSON schema on the classpath
     * (test resources, e.g. {@code testdata/schemas/user-list-schema.json}).
     *
     * @param response        the response
     * @param schemaResource  classpath path to the JSON schema
     */
    @Step("Validate JSON schema {schemaResource}")
    public static void validateSchema(Response response, String schemaResource) {
        LOG.info("Validating JSON schema from resource [{}]", schemaResource);
        response.then().assertThat()
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(schemaResource));
    }

    /**
     * Asserts a JSON-path field equals an expected value.
     *
     * @param response  the response
     * @param jsonPath  JSON path, e.g. {@code page}
     * @param expected  expected value
     */
    @Step("Validate field {jsonPath} equals {expected}")
    public static void validateField(Response response, String jsonPath, Object expected) {
        Object actual = response.jsonPath().get(jsonPath);
        LOG.info("Validating field | path=[{}] expected=[{}] actual=[{}]", jsonPath, expected, actual);
        Assert.assertEquals(actual, expected,
                "Field '" + jsonPath + "' mismatch");
    }

    /**
     * Asserts that a JSON-path field is present and non-null.
     *
     * @param response the response
     * @param jsonPath JSON path
     */
    public static void validateFieldNotNull(Response response, String jsonPath) {
        Object actual = response.jsonPath().get(jsonPath);
        LOG.info("Validating field not null | path=[{}] actual=[{}]", jsonPath, actual);
        Assert.assertNotNull(actual, "Field '" + jsonPath + "' should not be null");
    }

    /**
     * Asserts the response time is under a threshold.
     *
     * @param response    the response
     * @param maxTimeMs   maximum acceptable time in milliseconds
     */
    @Step("Validate response time < {maxTimeMs} ms")
    public static void validateResponseTimeLessThan(Response response, long maxTimeMs) {
        long time = response.getTime();
        LOG.info("Validating response time | actual=[{}ms] max=[{}ms]", time, maxTimeMs);
        Assert.assertTrue(time < maxTimeMs,
                "Response time " + time + "ms exceeded max " + maxTimeMs + "ms");
    }

    /**
     * Logs all key-value pairs extracted from the response body (useful for
     * debugging and Allure attachments).
     *
     * @param response the response
     */
    public static void logResponseFields(Response response) {
        Map<?, ?> map = response.jsonPath().getMap("$");
        if (map != null) {
            map.forEach((k, v) -> LOG.debug("Response field | {} = {}", k, v));
        }
    }
}
