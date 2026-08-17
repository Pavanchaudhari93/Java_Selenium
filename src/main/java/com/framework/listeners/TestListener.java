package com.framework.listeners;

import com.framework.base.DriverManager;
import com.framework.utils.ExtentLogger;
import com.framework.utils.ScreenshotUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG {@link ITestListener} that reports test lifecycle events.
 * <p>
 * On failure it captures and attaches a screenshot to Allure plus writes a
 * detailed error log. On success/skip it logs the outcome. Registered in
 * {@code testng.xml}.
 */
public class TestListener implements ITestListener {

    private static final Logger LOG = LogManager.getLogger(TestListener.class);

    @Override
    public void onTestStart(ITestResult result) {
        LOG.debug("Starting test [{}]", result.getName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LOG.info("PASSED | test [{}] | duration [{}ms]", result.getName(),
                result.getEndMillis() - result.getStartMillis());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = result.getName();
        LOG.error("FAILED | test [{}]", testName, result.getThrowable());

        if (DriverManager.isDriverInitialized()) {
            try {
                byte[] screenshot = ScreenshotUtils.captureScreenshot(DriverManager.getDriver());
                if (screenshot.length > 0) {
                    ExtentLogger.attachScreenshot("Failure screenshot - " + testName, screenshot);
                }
            } catch (Exception e) {
                LOG.warn("Could not capture failure screenshot for [{}]", testName, e);
            }
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LOG.warn("SKIPPED | test [{}] | reason: {}", result.getName(),
                result.getThrowable() != null ? result.getThrowable().getMessage() : "unknown");
    }

    @Override
    public void onStart(ITestContext context) {
        LOG.info("=== Starting TestNG context [{}] with [{}] test methods ===",
                context.getName(), context.getAllTestMethods().length);
    }

    @Override
    public void onFinish(ITestContext context) {
        LOG.info("=== Finished TestNG context [{}] | passed=[{}] failed=[{}] skipped=[{}] ===",
                context.getName(),
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());
    }
}
