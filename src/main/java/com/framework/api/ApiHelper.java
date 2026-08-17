package com.framework.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Thin wrapper around REST-assured providing reusable HTTP verbs.
 * <p>
 * All methods log and attach the traffic to Allure via the filters configured
 * in {@link ApiBase#getRequestSpec()}. Endpoints and base URIs are read from
 * config; callers pass relative paths such as {@code /users}.
 * <p>
 * Note: requests are sent via {@code RestAssured.given().spec(...)} because
 * invoking verbs directly on a {@code RequestSpecBuilder.build()} spec triggers
 * rest-assured issue #938 (NPE "Cannot get property 'assertionClosure'").
 */
public final class ApiHelper {

    private static final Logger LOG = LogManager.getLogger(ApiHelper.class);

    private ApiHelper() {
        // utility class - no instantiation
    }

    public static Response get(String endpoint) {
        return get(endpoint, null);
    }

    /**
     * Sends a GET request.
     *
     * @param endpoint    relative endpoint, e.g. {@code /users}
     * @param queryParams optional query params (may be {@code null})
     * @return REST-assured {@link Response}
     */
    public static Response get(String endpoint, Map<String, Object> queryParams) {
        LOG.info("GET {}", endpoint);
        RequestSpecification spec = ApiBase.getRequestSpec();
        if (queryParams != null && !queryParams.isEmpty()) {
            spec = spec.queryParams(queryParams);
        }
        return RestAssured.given().spec(spec)
                .when().get(endpoint)
                .then().extract().response();
    }

    /**
     * Sends a POST request with a POJO/Map body (serialized to JSON).
     *
     * @param endpoint relative endpoint
     * @param body     request body object
     * @return REST-assured {@link Response}
     */
    public static Response post(String endpoint, Object body) {
        LOG.info("POST {}", endpoint);
        return RestAssured.given().spec(ApiBase.getRequestSpec())
                .body(body)
                .when().post(endpoint)
                .then().extract().response();
    }

    /**
     * Sends a PUT request with a POJO/Map body (serialized to JSON).
     *
     * @param endpoint relative endpoint
     * @param body     request body object
     * @return REST-assured {@link Response}
     */
    public static Response put(String endpoint, Object body) {
        LOG.info("PUT {}", endpoint);
        return RestAssured.given().spec(ApiBase.getRequestSpec())
                .body(body)
                .when().put(endpoint)
                .then().extract().response();
    }

    /**
     * Sends a PATCH request with a POJO/Map body (serialized to JSON).
     *
     * @param endpoint relative endpoint
     * @param body     request body object
     * @return REST-assured {@link Response}
     */
    public static Response patch(String endpoint, Object body) {
        LOG.info("PATCH {}", endpoint);
        return RestAssured.given().spec(ApiBase.getRequestSpec())
                .body(body)
                .when().patch(endpoint)
                .then().extract().response();
    }

    /**
     * Sends a DELETE request.
     *
     * @param endpoint relative endpoint
     * @return REST-assured {@link Response}
     */
    public static Response delete(String endpoint) {
        LOG.info("DELETE {}", endpoint);
        return RestAssured.given().spec(ApiBase.getRequestSpec())
                .when().delete(endpoint)
                .then().extract().response();
    }
}
