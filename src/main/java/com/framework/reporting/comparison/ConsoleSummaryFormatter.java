package com.framework.reporting.comparison;

import java.util.List;
import java.util.Locale;

/**
 * Renders the plain-text summary printed to the console / CI log.
 */
public class ConsoleSummaryFormatter {

    private static final String RULE = "=".repeat(50);
    private static final int MAX_LISTED_FAILURES = 50;
    private static final int MESSAGE_MAX_CHARS = 200;

    public String format(ComparisonResult r) {
        StringBuilder out = new StringBuilder();
        line(out, RULE);
        line(out, "ALLURE EXECUTION COMPARISON REPORT");
        line(out, RULE);
        line(out, "");
        row(out, "Previous Run Total", r.previousTotal());
        row(out, "Current Run Total", r.currentTotal());
        line(out, "");
        for (ChangeType type : ChangeType.values()) {
            if (type != ChangeType.NO_CHANGE) {
                row(out, type.label(), r.count(type));
            }
        }
        row(out, "Unchanged", r.count(ChangeType.NO_CHANGE));
        line(out, "");
        row(out, "Pass Rate Previous", pct(r.passRatePrevious()));
        row(out, "Pass Rate Current", pct(r.passRateCurrent()));
        row(out, "Pass Rate Change", String.format(Locale.ROOT, "%+.1f pp (%s)", r.passRateDelta(), r.trend()));
        row(out, "Regression Rate", pct(r.regressionPercentage()) + " of previously passing tests");
        row(out, "Improvement Rate", pct(r.improvementPercentage()) + " of previously non-passing tests");
        line(out, "");
        appendFailures(out, r.currentFailures());
        line(out, RULE);
        return out.toString();
    }

    private static void appendFailures(StringBuilder out, List<TestComparison> failures) {
        if (failures.isEmpty()) {
            return;
        }
        line(out, "CURRENT RUN FAILURES (" + failures.size() + ")");
        failures.stream().limit(MAX_LISTED_FAILURES).forEach(c -> line(out,
                String.format(Locale.ROOT, " - %s [%s, %s]: %s", c.testName(), c.currentStatusLabel(),
                        c.changeType().label(), TextUtils.firstLine(c.failureMessage(), MESSAGE_MAX_CHARS))));
        if (failures.size() > MAX_LISTED_FAILURES) {
            line(out, " ... and " + (failures.size() - MAX_LISTED_FAILURES) + " more (see comparison-report.html)");
        }
        line(out, "");
    }

    private static void row(StringBuilder out, String label, Object value) {
        line(out, String.format(Locale.ROOT, "%-20s: %s", label, value));
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append(System.lineSeparator());
    }

    private static String pct(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }
}