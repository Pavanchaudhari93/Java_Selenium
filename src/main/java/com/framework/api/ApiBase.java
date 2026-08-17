package com.framework.api;

import com.framework.config.ConfigReader;
import com.framework.security.ZapHelper;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * REST-assured request specification factory.
 * <p>
 * Builds a shared {@link RequestSpecification} from config (base URI, base
 * path, content type, default headers). Every request flows through:
 * <ul>
 *   <li>{@link AllureRestAssured} - attaches request/response to the Allure report.</li>
 *   <li>{@link Log4j2RequestResponseFilter} - logs request/response via Log4j2.</li>
 * </ul>
 */
public final class ApiBase {

    private static final Logger LOG = LogManager.getLogger(ApiBase.class);

    private ApiBase() {
        // utility class - no instantiation
    }

    /**
     * @return a configured {@link RequestSpecification} for API calls.
     */
    public static RequestSpecification getRequestSpec() {
        String baseUri = ConfigReader.get("api.baseUri", "https://reqres.in");
        String basePath = ConfigReader.get("api.basePath", "/api");
        String contentType = ConfigReader.get("api.contentType", "application/json");

        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setBasePath(basePath)
                .setContentType(ContentType.JSON)
                .addHeader("Accept", contentType)
                .addFilter(new AllureRestAssured())
                .addFilter(new Log4j2RequestResponseFilter());

        applyDefaultHeaders(builder);
        applyZapProxyIfEnabled(builder);

        LOG.debug("API request spec configured: baseUri=[{}] basePath=[{}]", baseUri, basePath);
        return builder.build();
    }

    /**
     * When {@code security.zap.proxy.enabled=true}, routes REST-assured requests through
     * the ZAP proxy (derived from {@code security.zap.url}) so ZAP can passively record
     * API traffic, and relaxes TLS validation so ZAP's MITM certificate on HTTPS targets
     * doesn't fail the request.
     *
     * @param builder request spec builder being assembled for {@link #getRequestSpec()}
     */
    private static void applyZapProxyIfEnabled(RequestSpecBuilder builder) {
        if (!ZapHelper.isProxyModeEnabled()) {
            return;
        }
        if (!ZapHelper.isProxyListening()) {
            LOG.warn("security.zap.proxy.enabled=true but ZAP is not listening at [{}] - " +
                    "continuing without routing API traffic through it", ZapHelper.getProxyAddress());
            return;
        }
        builder.setProxy(ZapHelper.getProxyHost(), ZapHelper.getProxyPort());
        builder.setConfig(RestAssuredConfig.config()
                .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation()));
        LOG.info("Routing API traffic through ZAP proxy at [{}]", ZapHelper.getProxyAddress());
    }

    private static void applyDefaultHeaders(RequestSpecBuilder builder) {
        Map<String, String> headers = ConfigReader.get("api.defaultHeaders", "")
                .isEmpty() ? Map.of() : parseHeaders(ConfigReader.get("api.defaultHeaders"));
        headers.forEach(builder::addHeader);
    }

    private static Map<String, String> parseHeaders(String commaSeparated) {
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        for (String pair : commaSeparated.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                headers.put(kv[0].trim(), kv[1].trim());
            }
        }
        return headers;
    }

    /**
     * RestAssured {@link Filter} that logs request and response via Log4j2.
     */
    private static final class Log4j2RequestResponseFilter implements Filter {

        @Override
        public Response filter(FilterableRequestSpecification requestSpec,
                               FilterableResponseSpecification responseSpec,
                               FilterContext ctx) {
            LOG.info("HTTP {} {}", requestSpec.getMethod(), requestSpec.getURI());
            if (requestSpec.getBody() != null) {
                LOG.debug("Request body: {}", requestSpec.getBody().toString());
            }
            Response response = ctx.next(requestSpec, responseSpec);
            LOG.info("Response status: {}", response.getStatusCode());
            LOG.debug("Response body: {}", response.getBody().asString());
            return response;
        }
    }
}
