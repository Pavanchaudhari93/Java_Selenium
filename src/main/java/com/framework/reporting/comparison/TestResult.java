package com.framework.reporting.comparison;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * The subset of an Allure {@code *-result.json} file needed for run-to-run comparison.
 *
 * <p>{@code consoleLog} is not part of the JSON: {@link AllureParser} fills it from the
 * "Console log" attachment file, when a failed test has one.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TestResult(
        String historyId,
        String name,
        TestStatus status,
        long start,
        long stop,
        String fullName,
        StatusDetails statusDetails,
        List<Attachment> attachments,
        @JsonIgnore String consoleLog) {

    /** Allure's {@code statusDetails}: assertion message and stack trace of a failed/broken test. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatusDetails(String message, String trace) {
    }

    /** One entry of Allure's {@code attachments}; {@code source} is the file name in the results folder. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Attachment(String name, String source, String type) {
    }

    public TestResult {
        status = status == null ? TestStatus.UNKNOWN : status;
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }

    /**
     * Key used to match the same test across runs. Allure always writes {@code historyId};
     * {@code fullName} is a fallback for hand-edited or truncated result files.
     */
    public String comparisonKey() {
        if (historyId != null && !historyId.isBlank()) {
            return historyId;
        }
        return fullName;
    }

    /** Best human-readable name: fully qualified when available. */
    public String displayName() {
        return fullName != null && !fullName.isBlank() ? fullName : name;
    }

    public long durationMillis() {
        return Math.max(0, stop - start);
    }

    public boolean isFailure() {
        return status == TestStatus.FAILED || status == TestStatus.BROKEN;
    }

    public String failureMessage() {
        return statusDetails == null ? null : statusDetails.message();
    }

    public String failureTrace() {
        return statusDetails == null ? null : statusDetails.trace();
    }

    public TestResult withConsoleLog(String log) {
        return new TestResult(historyId, name, status, start, stop, fullName, statusDetails, attachments, log);
    }
}