package com.framework.reporting.comparison;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Matches tests across two runs by {@code historyId} and classifies each status change.
 */
public class AllureComparator {

    private static final Logger log = LogManager.getLogger(AllureComparator.class);

    /** Changes first (in {@link ChangeType} order), then alphabetically by test name. */
    private static final Comparator<TestComparison> REPORT_ORDER =
            Comparator.comparing(TestComparison::changeType)
                    .thenComparing(TestComparison::testName, Comparator.nullsLast(String::compareTo));

    public ComparisonResult compare(Map<String, TestResult> previous, Map<String, TestResult> current) {
        Set<String> allKeys = new LinkedHashSet<>(previous.keySet());
        allKeys.addAll(current.keySet());

        List<TestComparison> comparisons = allKeys.stream()
                .map(key -> toComparison(key, previous.get(key), current.get(key)))
                .sorted(REPORT_ORDER)
                .toList();

        ComparisonResult result = new ComparisonResult(
                comparisons,
                previous.size(),
                current.size(),
                countPassed(previous),
                countPassed(current));

        log.info("Compared {} previous vs {} current tests: {} regressions, {} fixes, {} new, {} removed",
                result.previousTotal(), result.currentTotal(), result.regressionCount(), result.fixCount(),
                result.count(ChangeType.NEW_TEST), result.count(ChangeType.REMOVED_TEST));
        return result;
    }

    private static TestComparison toComparison(String key, TestResult previous, TestResult current) {
        TestResult reference = current != null ? current : previous;
        TestStatus previousStatus = previous == null ? null : previous.status();
        TestStatus currentStatus = current == null ? null : current.status();
        boolean currentFailure = current != null && current.isFailure();
        return new TestComparison(
                key,
                reference.displayName(),
                previousStatus,
                currentStatus,
                ChangeType.of(previousStatus, currentStatus),
                currentFailure ? current.failureMessage() : null,
                currentFailure ? current.failureTrace() : null,
                currentFailure ? current.consoleLog() : null);
    }

    private static long countPassed(Map<String, TestResult> results) {
        return results.values().stream()
                .filter(r -> r.status() == TestStatus.PASSED)
                .count();
    }
}