package com.framework.utils;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Logging + Allure attachment helper.
 * <p>
 * Central place for structured logging (Log4j2) and Allure step/attachment
 * creation. Screenshots, JSON payloads, DB results and HTML reports can be
 * attached to the running Allure test case.
 */
public final class ExtentLogger {

    private static final Logger LOG = LogManager.getLogger(ExtentLogger.class);

    private ExtentLogger() {
        // utility class - no instantiation
    }

    public static void logInfo(String message) {
        LOG.info(message);
    }

    public static void logDebug(String message) {
        LOG.debug(message);
    }

    public static void logWarn(String message) {
        LOG.warn(message);
    }

    public static void logError(String message) {
        LOG.error(message);
    }

    public static void logError(String message, Throwable throwable) {
        LOG.error(message, throwable);
    }

    /**
     * Logs a human-readable step; visible as a step in the Allure report.
     *
     * @param description step description
     */
    @Step("{0}")
    public static void step(String description) {
        LOG.info(description);
    }

    /**
     * Attaches a plain-text payload to the current Allure test case.
     *
     * @param name    attachment name
     * @param content text content
     */
    public static void attachText(String name, String content) {
        Allure.addAttachment(name, "text/plain", content);
    }

    /**
     * Attaches a JSON payload to the current Allure test case.
     *
     * @param name attachment name
     * @param json JSON content
     */
    public static void attachJson(String name, String json) {
        Allure.addAttachment(name, "application/json", json);
    }

    /**
     * Attaches a PNG screenshot to the current Allure test case.
     *
     * @param name    attachment name
     * @param content PNG bytes
     */
    public static void attachScreenshot(String name, byte[] content) {
        if (content != null && content.length > 0) {
            Allure.addAttachment(name, "image/png", new java.io.ByteArrayInputStream(content), "png");
        }
    }

    /**
     * Attaches an HTML payload (e.g. a ZAP report) to the current Allure test case.
     *
     * @param name attachment name
     * @param html HTML content
     */
    public static void attachHtml(String name, String html) {
        Allure.addAttachment(name, "text/html", html);
    }
}
