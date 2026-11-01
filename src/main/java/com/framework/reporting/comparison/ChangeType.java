package com.framework.reporting.comparison;

/**
 * Classification of how a single test changed between the previous and current run.
 * Declaration order is the display order in reports (most important first).
 */
public enum ChangeType {
    PASSED_TO_FAILED("Passed -> Failed"),
    PASSED_TO_BROKEN("Passed -> Broken"),
    PASSED_TO_SKIPPED("Passed -> Skipped"),
    FAILED_TO_PASSED("Failed -> Passed"),
    BROKEN_TO_PASSED("Broken -> Passed"),
    SKIPPED_TO_PASSED("Skipped -> Passed"),
    OTHER_STATUS_CHANGE("Other Status Change"),
    NEW_TEST("New Test"),
    REMOVED_TEST("Removed Test"),
    NO_CHANGE("No Change");

    private final String label;

    ChangeType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** A previously passing test that now fails or breaks. */
    public boolean isRegression() {
        return this == PASSED_TO_FAILED || this == PASSED_TO_BROKEN;
    }

    /** A previously non-passing test that now passes. */
    public boolean isFix() {
        return this == FAILED_TO_PASSED || this == BROKEN_TO_PASSED || this == SKIPPED_TO_PASSED;
    }

    public static ChangeType of(TestStatus previous, TestStatus current) {
        if (previous == null) {
            return NEW_TEST;
        }
        if (current == null) {
            return REMOVED_TEST;
        }
        if (previous == current) {
            return NO_CHANGE;
        }
        if (previous == TestStatus.PASSED) {
            return switch (current) {
                case FAILED -> PASSED_TO_FAILED;
                case BROKEN -> PASSED_TO_BROKEN;
                case SKIPPED -> PASSED_TO_SKIPPED;
                default -> OTHER_STATUS_CHANGE;
            };
        }
        if (current == TestStatus.PASSED) {
            return switch (previous) {
                case FAILED -> FAILED_TO_PASSED;
                case BROKEN -> BROKEN_TO_PASSED;
                case SKIPPED -> SKIPPED_TO_PASSED;
                default -> OTHER_STATUS_CHANGE;
            };
        }
        return OTHER_STATUS_CHANGE;
    }
}