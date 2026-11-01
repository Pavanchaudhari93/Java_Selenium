package com.framework.reporting.comparison;

/**
 * Raised when Allure results cannot be read or comparison reports cannot be written.
 */
public class AllureComparisonException extends RuntimeException {

    public AllureComparisonException(String message) {
        super(message);
    }

    public AllureComparisonException(String message, Throwable cause) {
        super(message, cause);
    }
}