package com.framework.reporting.comparison;

/**
 * Comparison outcome for one test. Either status may be {@code null} for new / removed tests.
 *
 * <p>The failure fields come from the <em>current</em> run and are {@code null} unless the test
 * is Failed or Broken in that run.
 */
public record TestComparison(
        String historyId,
        String testName,
        TestStatus previousStatus,
        TestStatus currentStatus,
        ChangeType changeType,
        String failureMessage,
        String failureTrace,
        String consoleLog) {

    public String previousStatusLabel() {
        return previousStatus == null ? "" : previousStatus.label();
    }

    public String currentStatusLabel() {
        return currentStatus == null ? "" : currentStatus.label();
    }

    public boolean isCurrentFailure() {
        return currentStatus == TestStatus.FAILED || currentStatus == TestStatus.BROKEN;
    }
}