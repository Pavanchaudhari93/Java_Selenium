package com.framework.base;

import com.framework.assertions.SoftAssertHelper;
import com.framework.config.ConfigReader;
import com.framework.utils.ExtentLogger;
import com.framework.utils.ScreenshotUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

/**
 * Base class for all test classes.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>{@link BeforeSuite} - load the environment configuration once.</li>
 *   <li>{@link BeforeMethod} - initialize the (thread-local) WebDriver and
 *       navigate to the base URL for UI tests. API/security/db tests skip browser setup.</li>
 *   <li>{@link AfterMethod} - capture + attach a screenshot on failure, quit the
 *       driver and reset the soft-assert state.</li>
 * </ul>
 * Supports parallel execution ({@code parallel="methods"} in testng.xml) because
 * all driver state is thread-local via {@link DriverManager}.
 */
public abstract class BaseTest {

    private static final Logger LOG = LogManager.getLogger(BaseTest.class);

    /**
     * Loads configuration once before the whole suite starts.
     */
    @BeforeSuite(alwaysRun = true)
    public void setUpSuite() {
        ConfigReader.loadConfig();
        LOG.info("=== Test suite started | env=[{}] | browser=[{}] | execution.mode=[{}] ===",
                ConfigReader.getEnv(), ConfigReader.get("browser"), ConfigReader.get("execution.mode"));
    }

    /**
     * Per-test setup: initializes the browser and navigates to the base URL,
     * unless the test belongs to a non-UI group (api, security, db).
     *
     * @param method the test method about to run
     */
    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        if (isUiTest(method)) {
            DriverManager.initDriver();
            String baseUrl = ConfigReader.get("app.baseUrl");
            DriverManager.getDriver().get(baseUrl);
            LOG.info("Navigated to [{}] for test [{}]", baseUrl, method.getName());
        } else {
            LOG.info("Non-UI test [{}] - browser not required", method.getName());
        }
        ExtentLogger.step("Executing test: " + method.getName());
    }

    /**
     * Per-test teardown: screenshot on failure, quit driver, reset soft asserts.
     *
     * @param result result of the completed test
     */
    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        String status = result.isSuccess()
                ? "PASSED"
                : (result.getStatus() == ITestResult.SKIP ? "SKIPPED" : "FAILED");
        LOG.info("Test [{}] -> {}", result.getName(), status);

        if (!result.isSuccess() && DriverManager.isDriverInitialized()) {
            try {
                byte[] screenshot = ScreenshotUtils.captureScreenshot(DriverManager.getDriver());
                if (screenshot.length > 0) {
                    ExtentLogger.attachScreenshot("Screenshot on failure - " + result.getName(), screenshot);
                }
            } catch (Exception e) {
                LOG.warn("Could not capture failure screenshot for [{}]", result.getName(), e);
            }
        }
        DriverManager.quitDriver();
        SoftAssertHelper.reset();
    }

    /**
     * @return the WebDriver for the current test thread.
     */
    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }

    /**
     * Determines whether the test requires a browser. Tests belonging to
     * api/security/db groups are considered non-UI.
     *
     * @param method test method
     * @return {@code true} for UI tests
     */
    private boolean isUiTest(Method method) {
        Test testAnnotation = method.getAnnotation(Test.class);
        String[] groups = testAnnotation != null ? testAnnotation.groups() : new String[0];
        for (String group : groups) {
            if ("api".equalsIgnoreCase(group) || "security".equalsIgnoreCase(group) || "db".equalsIgnoreCase(group)) {
                return false;
            }
        }
        return true;
    }
}
