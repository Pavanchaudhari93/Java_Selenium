package com.framework.listeners;

import com.framework.config.ConfigReader;
import com.framework.utils.AllureBaselineManager;
import io.qameta.allure.testng.AllureTestNg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IExecutionListener;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Allure integration listener.
 * <p>
 * Extends the official {@link AllureTestNg} (which instruments steps,
 * attachments and test status) and additionally writes
 * {@code environment.properties} before execution so the Allure report shows
 * the browser, environment and execution mode used for the run.
 * <p>
 * Also archives the previous run's Allure results for run-to-run comparison
 * via {@link AllureBaselineManager}.
 */
public class AllureListener extends AllureTestNg implements IExecutionListener {

    private static final Logger LOG = LogManager.getLogger(AllureListener.class);

    @Override
    public void onExecutionStart() {
        // Archive previous run's results for comparison before this run starts
        AllureBaselineManager.archivePreviousAllureResults();

        try {
            String resultsDir = System.getProperty("allure.results.directory", "target/allure-results");
            Path dir = Paths.get(resultsDir);
            Files.createDirectories(dir);

            Properties environment = new Properties();
            environment.setProperty("Environment", ConfigReader.getEnv());
            environment.setProperty("Browser", ConfigReader.get("browser", "chrome"));
            environment.setProperty("Execution Mode", ConfigReader.get("execution.mode", "local"));
            environment.setProperty("Headless", String.valueOf(ConfigReader.getBoolean("browser.headless", false)));

            try (OutputStream out = Files.newOutputStream(dir.resolve("environment.properties"))) {
                environment.store(out, "Allure environment properties");
            }
            LOG.info("Allure environment.properties written to [{}]", dir);
        } catch (IOException e) {
            LOG.warn("Could not write Allure environment.properties", e);
        }
    }

    @Override
    public void onExecutionFinish() {
        // nothing to clean up for this listener
    }
}
