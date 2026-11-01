package com.framework.reporting.comparison;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes {@code comparison-report.csv}: one row per test across both runs.
 */
public class CsvReportGenerator implements ReportGenerator {

    public static final String FILE_NAME = "comparison-report.csv";

    private static final Logger log = LogManager.getLogger(CsvReportGenerator.class);
    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader("Test Name", "History ID", "Previous Status", "Current Status", "Change Type", "Failure Message")
            .build();
    // Excel caps a cell at 32,767 characters; long assertion dumps are cut well below that.
    private static final int MESSAGE_MAX_CHARS = 2000;

    @Override
    public Path generate(ComparisonResult result, Path outputDir) {
        Path file = outputDir.resolve(FILE_NAME);
        try {
            Files.createDirectories(outputDir);
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(writer, FORMAT)) {
                for (TestComparison c : result.comparisons()) {
                    printer.printRecord(
                            c.testName(),
                            c.historyId(),
                            c.previousStatusLabel(),
                            c.currentStatusLabel(),
                            c.changeType().label(),
                            TextUtils.truncate(c.failureMessage(), MESSAGE_MAX_CHARS));
                }
            }
        } catch (IOException e) {
            throw new AllureComparisonException("Failed to write CSV report " + file, e);
        }
        log.info("CSV report written: {}", file.toAbsolutePath());
        return file;
    }
}