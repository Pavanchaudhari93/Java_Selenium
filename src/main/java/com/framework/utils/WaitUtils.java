package com.framework.utils;

import com.framework.config.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Static helpers for explicit waits and page-ready checks.
 * <p>
 * All waits use the configurable {@code timeout.explicit} value and log clearly.
 * No fixed {@code Thread.sleep} calls are used except {@link #sleep(long)}
 * which is reserved for justified edge cases and logs a warning.
 */
public final class WaitUtils {

    private static final Logger LOG = LogManager.getLogger(WaitUtils.class);

    private WaitUtils() {
        // utility class - no instantiation
    }

    private static WebDriverWait newWait(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(ConfigReader.getInt("timeout.explicit", 30)));
    }

    private static WebDriverWait newWait(WebDriver driver, long timeoutSeconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    }

    // ------------------------------------------------------------------
    // Element visibility / clickability
    // ------------------------------------------------------------------

    public static WebElement waitForElementVisible(WebDriver driver, By locator) {
        return newWait(driver).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForElementVisible(WebDriver driver, By locator, long timeoutSeconds) {
        return newWait(driver, timeoutSeconds).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForElementVisible(WebDriver driver, WebElement element) {
        return newWait(driver).until(ExpectedConditions.visibilityOf(element));
    }

    public static WebElement waitForElementClickable(WebDriver driver, By locator) {
        return newWait(driver).until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static WebElement waitForElementClickable(WebDriver driver, WebElement element) {
        return newWait(driver).until(ExpectedConditions.elementToBeClickable(element));
    }

    public static void waitForElementToDisappear(WebDriver driver, By locator) {
        newWait(driver).until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public static void waitForElementToDisappear(WebDriver driver, By locator, long timeoutSeconds) {
        newWait(driver, timeoutSeconds).until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    // ------------------------------------------------------------------
    // Text / attribute conditions
    // ------------------------------------------------------------------

    public static void waitForTextToBePresent(WebDriver driver, By locator, String text) {
        newWait(driver).until(ExpectedConditions.textToBe(locator, text));
    }

    public static void waitForTextInElement(WebDriver driver, WebElement element, String text) {
        newWait(driver).until(ExpectedConditions.textToBePresentInElement(element, text));
    }

    public static void waitForAttributeToContain(WebDriver driver, By locator, String attribute, String value) {
        newWait(driver).until(ExpectedConditions.attributeContains(locator, attribute, value));
    }

    // ------------------------------------------------------------------
    // URL / title / window
    // ------------------------------------------------------------------

    public static void waitForUrlToContain(WebDriver driver, String fragment) {
        newWait(driver).until(ExpectedConditions.urlContains(fragment));
    }

    public static void waitForNumberOfWindows(WebDriver driver, int expectedCount) {
        newWait(driver).until(ExpectedConditions.numberOfWindowsToBe(expectedCount));
    }

    public static void waitForTitle(WebDriver driver, String title) {
        newWait(driver).until(ExpectedConditions.titleIs(title));
    }

    // ------------------------------------------------------------------
    // Alerts
    // ------------------------------------------------------------------

    public static Alert waitForAlert(WebDriver driver) {
        return newWait(driver).until(ExpectedConditions.alertIsPresent());
    }

    // ------------------------------------------------------------------
    // Page readiness
    // ------------------------------------------------------------------

    /**
     * Waits for {@code document.readyState == 'complete'}.
     *
     * @param driver WebDriver
     */
    public static void waitForPageLoad(WebDriver driver) {
        try {
            newWait(driver, 60).until((ExpectedCondition<Boolean>) d -> d != null
                    && "complete".equals(((JavascriptExecutor) d)
                    .executeScript("return document.readyState")));
        } catch (TimeoutException e) {
            LOG.warn("Page did not reach readyState 'complete' within timeout");
        }
    }

    // ------------------------------------------------------------------
    // Polling
    // ------------------------------------------------------------------

    /**
     * Polls a boolean condition until it becomes {@code true} or times out.
     *
     * @param driver     WebDriver
     * @param condition  the condition to poll
     * @param timeoutSeconds timeout in seconds
     * @return {@code true} if the condition was satisfied
     */
    public static boolean waitForCondition(WebDriver driver, ExpectedCondition<Boolean> condition, long timeoutSeconds) {
        try {
            return newWait(driver, timeoutSeconds).until(condition);
        } catch (TimeoutException e) {
            LOG.warn("Condition not satisfied within [{}]s", timeoutSeconds);
            return false;
        }
    }

    /**
     * Indicates whether an element exists in the DOM without failing the test.
     *
     * @param driver  WebDriver
     * @param locator By locator
     * @return {@code true} if present
     */
    public static boolean isElementPresent(WebDriver driver, By locator) {
        try {
            driver.findElement(locator);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Explicit pause. RESERVED for genuinely justified edge cases (e.g. waiting
     * on a third-party animation). Prefer explicit waits instead.
     *
     * @param milliseconds sleep duration
     */
    public static void sleep(long milliseconds) {
        LOG.warn("Justified explicit sleep of [{}]ms - prefer an explicit WebDriverWait", milliseconds);
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Sleep interrupted", e);
        }
    }
}
