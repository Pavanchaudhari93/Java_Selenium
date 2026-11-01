package com.framework.reporting.comparison;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shortens failure messages, stack traces and logs so the reports stay readable and small.
 */
final class TextUtils {

    private TextUtils() {
    }

    private static final String PLAYWRIGHT_MESSAGE_PREFIX = "message='";

    /**
     * One-line summary of a failure message, cut to {@code maxChars}; "" for null/blank input.
     * Playwright errors begin with a bare "Error {" line and carry the real text on a
     * {@code message='...'} line, so that line is preferred when present.
     */
    static String firstLine(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        List<String> lines = Arrays.stream(text.split("\\R"))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();
        String line = lines.stream()
                .filter(s -> s.startsWith(PLAYWRIGHT_MESSAGE_PREFIX))
                .map(s -> s.substring(PLAYWRIGHT_MESSAGE_PREFIX.length()))
                .findFirst()
                .orElse(lines.isEmpty() ? "" : lines.get(0));
        return truncate(line, maxChars);
    }

    /** At most {@code maxLines} leading lines, with a marker saying how many were dropped. */
    static String headLines(String text, int maxLines) {
        if (text == null) {
            return "";
        }
        String[] lines = text.split("\\R");
        if (lines.length <= maxLines) {
            return text;
        }
        return Arrays.stream(lines, 0, maxLines).collect(Collectors.joining(System.lineSeparator()))
                + System.lineSeparator() + "... (" + (lines.length - maxLines) + " more lines)";
    }

    static int lineCount(String text) {
        return text == null || text.isEmpty() ? 0 : text.split("\\R").length;
    }

    static String truncate(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxChars ? text : text.substring(0, maxChars) + "...";
    }
}