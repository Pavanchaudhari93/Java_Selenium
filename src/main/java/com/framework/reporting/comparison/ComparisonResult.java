package com.framework.reporting.comparison;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Immutable outcome of comparing two Allure runs, including summary and trend statistics.
 */
public final class ComparisonResult {

    /** Direction of the overall pass rate between runs. */
    public enum Trend { IMPROVED, REGRESSED, STABLE }

    /** Pass-rate changes smaller than this (in percentage points) count as {@link Trend#STABLE}. */
    private static final double STABLE_TOLERANCE = 0.05;

    private final List<TestComparison> comparisons;
    private final Map<ChangeType, Long> countsByType;
    private final int previousTotal;
    private final int currentTotal;
    private final long previousPassed;
    private final long currentPassed;

    public ComparisonResult(List<TestComparison> comparisons,
                            int previousTotal, int currentTotal,
                            long previousPassed, long currentPassed) {
        this.comparisons = List.copyOf(Objects.requireNonNull(comparisons, "comparisons"));
        this.previousTotal = previousTotal;
        this.currentTotal = currentTotal;
        this.previousPassed = previousPassed;
        this.currentPassed = currentPassed;

        Map<ChangeType, Long> counts = new EnumMap<>(ChangeType.class);
        for (ChangeType type : ChangeType.values()) {
            counts.put(type, 0L);
        }
        this.comparisons.forEach(c -> counts.merge(c.changeType(), 1L, Long::sum));
        this.countsByType = Collections.unmodifiableMap(counts);
    }

    public List<TestComparison> comparisons() {
        return comparisons;
    }

    public List<TestComparison> byType(ChangeType type) {
        return filter(c -> c.changeType() == type);
    }

    /** Tests that are Failed or Broken in the current run, regressions first (report order). */
    public List<TestComparison> currentFailures() {
        return filter(TestComparison::isCurrentFailure);
    }

    public long count(ChangeType type) {
        return countsByType.get(type);
    }

    public Map<ChangeType, Long> countsByType() {
        return countsByType;
    }

    public int previousTotal() {
        return previousTotal;
    }

    public int currentTotal() {
        return currentTotal;
    }

    public long previousPassed() {
        return previousPassed;
    }

    public long currentPassed() {
        return currentPassed;
    }

    public double passRatePrevious() {
        return percent(previousPassed, previousTotal);
    }

    public double passRateCurrent() {
        return percent(currentPassed, currentTotal);
    }

    /** Pass-rate change in percentage points (positive = improvement). */
    public double passRateDelta() {
        return passRateCurrent() - passRatePrevious();
    }

    /** Passed -> Failed plus Passed -> Broken. */
    public long regressionCount() {
        return countWhere(ChangeType::isRegression);
    }

    /** Failed / Broken / Skipped -> Passed. */
    public long fixCount() {
        return countWhere(ChangeType::isFix);
    }

    /** Share of tests that passed previously (and still exist) which now fail or break. */
    public double regressionPercentage() {
        long previouslyPassingCommon = comparisons.stream()
                .filter(c -> c.previousStatus() == TestStatus.PASSED && c.currentStatus() != null)
                .count();
        return percent(regressionCount(), previouslyPassingCommon);
    }

    /** Share of tests that did not pass previously (and still exist) which now pass. */
    public double improvementPercentage() {
        long previouslyNonPassingCommon = comparisons.stream()
                .filter(c -> c.previousStatus() != null && c.previousStatus() != TestStatus.PASSED
                        && c.currentStatus() != null)
                .count();
        return percent(fixCount(), previouslyNonPassingCommon);
    }

    public Trend trend() {
        double delta = passRateDelta();
        if (Math.abs(delta) < STABLE_TOLERANCE) {
            return Trend.STABLE;
        }
        return delta > 0 ? Trend.IMPROVED : Trend.REGRESSED;
    }

    private long countWhere(Predicate<ChangeType> predicate) {
        return countsByType.entrySet().stream()
                .filter(e -> predicate.test(e.getKey()))
                .mapToLong(Map.Entry::getValue)
                .sum();
    }

    private List<TestComparison> filter(Predicate<TestComparison> predicate) {
        return comparisons.stream().filter(predicate).toList();
    }

    private static double percent(long part, long whole) {
        return whole == 0 ? 0.0 : (part * 100.0) / whole;
    }
}