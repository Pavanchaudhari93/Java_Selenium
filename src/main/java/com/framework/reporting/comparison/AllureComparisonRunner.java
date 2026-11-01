package com.framework.reporting.comparison;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Command-line entry point: compares two Allure result folders, prints a summary and writes
 * CSV + HTML reports.
 *
 * <pre>
 * Options (each also settable as a -D system property, CLI wins):
 *   --previous  <dir>   allure.compare.previous   default Execution_Reports/Allure_Previous/allure-results
 *   --current   <dir>   allure.compare.current    default Execution_Reports/Allure_Report/allure-results
 *   --output    <dir>   allure.compare.output     default Execution_Reports/Allure_Comparison
 *   --threshold <n>     allure.compare.threshold  max allowed Passed -> Failed (default -1 = no gate)
 *   --include-broken     allure.compare.includeBroken  also count Passed -> Broken against the threshold
 * </pre>
 *
 * Exit codes: 0 = OK, 1 = threshold exceeded, 2 = error (bad arguments, missing results, I/O).
 *
 * Run via Maven: {@code mvn test-compile exec:exec@allure-compare -Dallure.compare.threshold=5}
 */
public final class AllureComparisonRunner {

    public static final int EXIT_OK = 0;
    public static final int EXIT_THRESHOLD_EXCEEDED = 1;
    public static final int EXIT_ERROR = 2;

    private static final Logger log = LogManager.getLogger(AllureComparisonRunner.class);

    private final AllureParser parser;
    private final AllureComparator comparator;
    private final ConsoleSummaryFormatter summaryFormatter;
    private final List<ReportGenerator> generators;

    public AllureComparisonRunner(AllureParser parser, AllureComparator comparator,
                                  ConsoleSummaryFormatter summaryFormatter, List<ReportGenerator> generators) {
        this.parser = parser;
        this.comparator = comparator;
        this.summaryFormatter = summaryFormatter;
        this.generators = List.copyOf(generators);
    }

    public static void main(String[] args) {
        int exitCode;
        try {
            Config config = Config.from(args);
            AllureComparisonRunner runner = new AllureComparisonRunner(
                    new AllureParser(),
                    new AllureComparator(),
                    new ConsoleSummaryFormatter(),
                    List.of(new CsvReportGenerator(), new HtmlReportGenerator()));
            exitCode = runner.run(config);
        } catch (IllegalArgumentException | AllureComparisonException e) {
            log.error("Allure comparison failed: {}", e.getMessage());
            log.debug("Allure comparison failure details", e);
            exitCode = EXIT_ERROR;
        }
        System.exit(exitCode);
    }

    public int run(Config config) {
        log.info("Comparing previous={} current={}", config.previous().toAbsolutePath(), config.current().toAbsolutePath());

        Map<String, TestResult> previous = parser.parse(config.previous());
        Map<String, TestResult> current = parser.parse(config.current());
        if (current.isEmpty()) {
            throw new AllureComparisonException("Current run has no test results in " + config.current());
        }

        ComparisonResult result = comparator.compare(previous, current);
        System.out.println(summaryFormatter.format(result));
        generators.forEach(g -> g.generate(result, config.output()));

        return evaluateThreshold(result, config);
    }

    private static int evaluateThreshold(ComparisonResult result, Config config) {
        if (config.threshold() < 0) {
            return EXIT_OK;
        }
        long gated = result.count(ChangeType.PASSED_TO_FAILED)
                + (config.includeBroken() ? result.count(ChangeType.PASSED_TO_BROKEN) : 0);
        String metric = config.includeBroken() ? "Passed -> Failed/Broken" : "Passed -> Failed";
        if (gated > config.threshold()) {
            log.error("Quality gate FAILED: {} count {} exceeds threshold {}", metric, gated, config.threshold());
            return EXIT_THRESHOLD_EXCEEDED;
        }
        log.info("Quality gate passed: {} count {} within threshold {}", metric, gated, config.threshold());
        return EXIT_OK;
    }

    /** Resolved runner options. */
    public record Config(Path previous, Path current, Path output, int threshold, boolean includeBroken) {

        static Config from(String[] args) {
            String previous = System.getProperty("allure.compare.previous", "Execution_Reports/Allure_Previous/allure-results");
            String current = System.getProperty("allure.compare.current", "Execution_Reports/Allure_Report/allure-results");
            String output = System.getProperty("allure.compare.output", "Execution_Reports/Allure_Comparison");
            String threshold = System.getProperty("allure.compare.threshold", "-1");
            boolean includeBroken = Boolean.parseBoolean(System.getProperty("allure.compare.includeBroken", "false"));

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--previous" -> previous = value(args, ++i, arg);
                    case "--current" -> current = value(args, ++i, arg);
                    case "--output" -> output = value(args, ++i, arg);
                    case "--threshold" -> threshold = value(args, ++i, arg);
                    case "--include-broken" -> includeBroken = true;
                    default -> throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            return new Config(Path.of(previous), Path.of(current), Path.of(output), parseThreshold(threshold), includeBroken);
        }

        private static String value(String[] args, int index, String option) {
            if (index >= args.length || args[index].startsWith("--")) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }

        private static int parseThreshold(String raw) {
            String trimmed = raw == null ? "" : raw.trim();
            if (trimmed.isEmpty()) {
                return -1;
            }
            try {
                return Integer.parseInt(trimmed);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Threshold must be an integer, got: " + raw, e);
            }
        }
    }
}