package com.framework.reporting.comparison;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

/**
 * Allure test status. Any value Allure writes that is not recognised maps to {@link #UNKNOWN}.
 */
public enum TestStatus {
    PASSED("Passed"),
    FAILED("Failed"),
    BROKEN("Broken"),
    SKIPPED("Skipped"),
    UNKNOWN("Unknown");

    private final String label;

    TestStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @JsonCreator
    public static TestStatus fromAllure(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}