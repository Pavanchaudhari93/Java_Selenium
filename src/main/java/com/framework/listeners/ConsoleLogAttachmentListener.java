package com.framework.listeners;

import com.framework.utils.ExtentLogger;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * TestNG listener that captures console output (System.out/System.err) for failed/broken tests
 * and attaches it to the Allure report as "Console log".
 *
 * <p>This enables {@link com.framework.reporting.comparison.AllureParser} to include console
 * logs in the comparison reports for failed tests.
 */
public class ConsoleLogAttachmentListener implements ITestListener {

    private static final Logger LOG = LogManager.getLogger(ConsoleLogAttachmentListener.class);

    private static final ThreadLocal<ByteArrayOutputStream> OUTPUT_BUFFER = new ThreadLocal<>();
    private static final ThreadLocal<PrintStream> ORIGINAL_OUT = new ThreadLocal<>();
    private static final ThreadLocal<PrintStream> ORIGINAL_ERR = new ThreadLocal<>();

    @Override
    public void onTestStart(ITestResult result) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream combined = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        ORIGINAL_OUT.set(System.out);
        ORIGINAL_ERR.set(System.err);
        System.setOut(combined);
        System.setErr(combined);
        OUTPUT_BUFFER.set(buffer);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        cleanup();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        attachConsoleLog(result);
        cleanup();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        cleanup();
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        cleanup();
    }

    @Override
    public void onStart(ITestContext context) {
        // Not used
    }

    @Override
    public void onFinish(ITestContext context) {
        // Not used
    }

    private void attachConsoleLog(ITestResult result) {
        ByteArrayOutputStream buffer = OUTPUT_BUFFER.get();
        if (buffer == null || buffer.size() == 0) {
            return;
        }

        String consoleLog = buffer.toString(StandardCharsets.UTF_8);
        if (consoleLog.isBlank()) {
            return;
        }

        try {
            Allure.addAttachment("Console log", "text/plain", consoleLog, ".txt");
            ExtentLogger.attachText("Console log", consoleLog);
            LOG.debug("Attached console log to Allure for test: {}", result.getName());
        } catch (Exception e) {
            LOG.warn("Failed to attach console log for test {}: {}", result.getName(), e.getMessage());
        }
    }

    private void cleanup() {
        PrintStream out = ORIGINAL_OUT.get();
        PrintStream err = ORIGINAL_ERR.get();
        if (out != null) {
            System.setOut(out);
            ORIGINAL_OUT.remove();
        }
        if (err != null) {
            System.setErr(err);
            ORIGINAL_ERR.remove();
        }
        OUTPUT_BUFFER.remove();
    }
}