package com.framework.utils;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Screenshot capture utilities.
 * <p>
 * Captures screenshots as raw bytes (for Allure attachments) and/or persists
 * them under {@code target/screenshots}. Failure to capture is logged and a
 * zero-length array is returned rather than throwing, so screenshots never
 * break test execution.
 */
public final class ScreenshotUtils {

    private static final Logger LOG = LogManager.getLogger(ScreenshotUtils.class);

    private ScreenshotUtils() {
        // utility class - no instantiation
    }

    /**
     * Captures the current viewport as a byte array.
     *
     * @param driver WebDriver
     * @return PNG bytes, or an empty array when capture fails
     */
    public static byte[] captureScreenshot(WebDriver driver) {
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            LOG.warn("Screenshot capture failed: {}", e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Saves a screenshot to {@code target/screenshots/&lt;name&gt;_&lt;timestamp&gt;.png}.
     *
     * @param driver WebDriver
     * @param name   logical name of the screenshot
     * @return absolute path of the saved file, or {@code null} on failure
     */
    public static String saveScreenshot(WebDriver driver, String name) {
        try {
            byte[] bytes = captureScreenshot(driver);
            if (bytes.length == 0) {
                return null;
            }
            String directory = "target/screenshots";
            File dir = new File(directory);
            if (!dir.exists() && !dir.mkdirs()) {
                LOG.warn("Could not create screenshot directory [{}]", directory);
            }
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filePath = directory + "/" + sanitize(name) + "_" + timestamp + ".png";
            FileUtils.writeByteArrayToFile(new File(filePath), bytes);
            LOG.info("Screenshot saved to [{}]", filePath);
            return filePath;
        } catch (Exception e) {
            LOG.warn("Failed to save screenshot [{}]", name, e);
            return null;
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
