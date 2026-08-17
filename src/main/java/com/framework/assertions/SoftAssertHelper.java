package com.framework.assertions;

import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.asserts.SoftAssert;

import java.util.Objects;

/**
 * Wraps TestNG {@link SoftAssert} with logging and Allure step integration.
 * <p>
 * Each thread gets its own {@link SoftAssert} (ThreadLocal) so parallel test
 * methods do not interfere. Call {@link #assertAll()} at the end of a test to
 * flush all collected failures; {@link #reset()} is invoked automatically by
 * {@code BaseTest.tearDown}.
 */
public final class SoftAssertHelper {

    private static final Logger LOG = LogManager.getLogger(SoftAssertHelper.class);

    private static final ThreadLocal<SoftAssert> SOFT_ASSERT = ThreadLocal.withInitial(SoftAssert::new);

    private SoftAssertHelper() {
        // utility class - no instantiation
    }

    /**
     * Soft assertion for equality. Failure is recorded, not thrown.
     *
     * @param actual   actual value
     * @param expected expected value
     * @param message  assertion description
     */
    @Step("Soft assert equals | {message}")
    public static void assertEquals(Object actual, Object expected, String message) {
        boolean passed = Objects.equals(actual, expected);
        logResult(passed, message, "expected=[" + expected + "] actual=[" + actual + "]");
        SOFT_ASSERT.get().assertEquals(actual, expected, message);
    }

    /**
     * Soft assertion for a boolean condition.
     *
     * @param condition condition to check
     * @param message   assertion description
     */
    @Step("Soft assert true | {message}")
    public static void assertTrue(boolean condition, String message) {
        logResult(condition, message, "");
        SOFT_ASSERT.get().assertTrue(condition, message);
    }

    /**
     * Soft assertion for a negated boolean condition.
     *
     * @param condition condition that should be false
     * @param message   assertion description
     */
    @Step("Soft assert false | {message}")
    public static void assertFalse(boolean condition, String message) {
        logResult(!condition, message, "");
        SOFT_ASSERT.get().assertFalse(condition, message);
    }

    /**
     * Soft assertion that a value is not null.
     *
     * @param value   value to check
     * @param message assertion description
     */
    @Step("Soft assert not null | {message}")
    public static void assertNotNull(Object value, String message) {
        logResult(value != null, message, "value=[" + value + "]");
        SOFT_ASSERT.get().assertNotNull(value, message);
    }

    /**
     * Flushes all recorded soft assertions. Throws the aggregated
     * {@link AssertionError} if any assertion failed.
     */
    public static void assertAll() {
        LOG.info("Flushing all collected soft assertions");
        SOFT_ASSERT.get().assertAll();
    }

    /**
     * Clears the soft assertion state for the current thread.
     * Called automatically from {@code BaseTest.tearDown}.
     */
    public static void reset() {
        SOFT_ASSERT.remove();
    }

    private static void logResult(boolean passed, String message, String detail) {
        if (passed) {
            LOG.info("PASS | {}", message);
        } else {
            LOG.error("FAIL | {} | {}", message, detail);
        }
    }
}
