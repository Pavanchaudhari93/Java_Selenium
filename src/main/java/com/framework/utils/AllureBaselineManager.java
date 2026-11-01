package com.framework.utils;

import com.framework.config.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Utility to manage local Allure results baseline for run-to-run comparison.
 *
 * <p>This class archives the previous run's {@code allure-results} to a separate
 * directory before the new test run cleans the results directory. The archived
 * results are then used by {@link com.framework.reporting.comparison.AllureComparisonRunner}
 * as the "previous" run for comparison.
 *
 * <p>Usage: Call {@link #archivePreviousAllureResults()} at the start of the test suite
 * (e.g., in a {@code @BeforeSuite} method or TestNG listener) before the results
 * directory is cleaned.
 */
public final class AllureBaselineManager {

    private static final Logger LOG = LogManager.getLogger(AllureBaselineManager.class);

    private static final String ALLURE_RESULTS_DIR = "allure-results";

    // Archive only once per JVM run (testng.xml may have multiple <test> tags)
    private static final AtomicBoolean allureArchived = new AtomicBoolean(false);

    private AllureBaselineManager() {
    }

    /**
     * Archives the previous run's Allure results to the baseline directory.
     * <p>
     * This method is idempotent - it only runs once per JVM execution.
     * It is safe to call from multiple threads (e.g., parallel TestNG tests).
     */
    public static void archivePreviousAllureResults() {
        if (!allureArchived.compareAndSet(false, true)) {
            return; // Already archived in this JVM run
        }

        String allureReportPath = ConfigReader.get("allure.results.directory", "target/allure-results");
        String allurePreviousPath = ConfigReader.get("allure.previous.directory", "target/allure-previous");

        // The allure.results.directory may be absolute (set by surefire) or relative
        Path source = Paths.get(allureReportPath).toAbsolutePath();
        Path target = Paths.get(allurePreviousPath).toAbsolutePath();

        try {
            if (!hasTestResults(source)) {
                LOG.info("[STATUS] No previous Allure test results to archive in {}", source);
                return;
            }

            if (Files.exists(target)) {
                cleanDirectory(target);
            }
            Files.createDirectories(target);

            try (Stream<Path> files = Files.list(source)) {
                files.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith("-result.json"))
                        .forEach(file -> {
                            try {
                                Files.copy(file, target.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                LOG.warn("Failed to copy {} to baseline: {}", file.getFileName(), e.getMessage());
                            }
                        });
            }
            LOG.info("[STATUS] Previous Allure results archived to {}", target);

        } catch (IOException e) {
            LOG.error("[ERROR] Previous Allure results archive failure :: ", e);
        }
    }

    /**
     * Cleans the previous baseline directory.
     * Can be called to reset the baseline (e.g., on demand).
     */
    public static void clearBaseline() {
        String allurePreviousPath = ConfigReader.get("allure.previous.directory", "target/allure-previous");
        Path target = Paths.get(allurePreviousPath).toAbsolutePath();

        try {
            if (Files.exists(target)) {
                cleanDirectory(target);
                LOG.info("[STATUS] Allure baseline cleared at {}", target);
            }
        } catch (IOException e) {
            LOG.error("[ERROR] Failed to clear Allure baseline :: ", e);
        }
    }

    private static boolean hasTestResults(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.anyMatch(p -> p.getFileName().toString().endsWith("-result.json"));
        }
    }

    private static void cleanDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isRegularFile).forEach(file -> {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    LOG.warn("Failed to delete {}: {}", file.getFileName(), e.getMessage());
                }
            });
        }
    }
}