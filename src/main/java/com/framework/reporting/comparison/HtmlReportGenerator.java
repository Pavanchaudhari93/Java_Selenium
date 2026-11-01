package com.framework.reporting.comparison;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Writes {@code comparison-report.html}: a self-contained page (no external assets) with the
 * execution summary, trend, status-change statistics, new/removed tests and a filterable
 * detailed comparison table.
 */
public class HtmlReportGenerator implements ReportGenerator {

    public static final String FILE_NAME = "comparison-report.html";

    private static final Logger log = LogManager.getLogger(HtmlReportGenerator.class);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String STYLE = """
            body{font-family:Segoe UI,Arial,sans-serif;margin:24px;color:#1f2933;background:#f7f9fb}
            h1{margin-bottom:4px}h2{margin-top:32px;border-bottom:1px solid #d9e2ec;padding-bottom:6px}
            .meta{color:#627d98;font-size:13px}
            .cards{display:flex;flex-wrap:wrap;gap:12px;margin-top:16px}
            .card{background:#fff;border:1px solid #d9e2ec;border-radius:6px;padding:12px 16px;min-width:150px}
            .card .v{font-size:24px;font-weight:600}.card .k{font-size:12px;color:#627d98;text-transform:uppercase}
            table{border-collapse:collapse;width:100%;background:#fff;font-size:13px}
            th,td{border:1px solid #d9e2ec;padding:6px 8px;text-align:left;vertical-align:top}
            th{background:#f0f4f8;position:sticky;top:0}
            td.id{font-family:Consolas,monospace;color:#627d98}
            .passed{color:#1e7b34}.failed{color:#c62828}.broken{color:#b26a00}.skipped{color:#627d98}.unknown{color:#627d98}
            .IMPROVED{color:#1e7b34}.REGRESSED{color:#c62828}.STABLE{color:#627d98}
            tr.regression{background:#fdecea}tr.fix{background:#e8f5e9}
            input{padding:6px 8px;width:320px;margin:8px 0;border:1px solid #bcccdc;border-radius:4px}
            .empty{color:#627d98;font-style:italic}
            .msg{color:#c62828;white-space:pre-wrap;word-break:break-word;margin:0 0 6px}
            details{margin:4px 0}summary{cursor:pointer;color:#334e68;font-size:12px}
            pre.log{background:#102a43;color:#d9e2ec;padding:8px;max-height:360px;overflow:auto;font-size:12px;white-space:pre-wrap;word-break:break-word}
            """;

    private static final int TRACE_MAX_LINES = 40;
    private static final int DETAIL_MESSAGE_MAX_CHARS = 300;

    private static final String FILTER_SCRIPT = """
            function filterRows(q){q=q.toLowerCase();document.querySelectorAll('#details tbody tr').forEach(function(r){
            r.style.display=r.textContent.toLowerCase().indexOf(q)>=0?'':'none';});}
            """;

    @Override
    public Path generate(ComparisonResult result, Path outputDir) {
        Path file = outputDir.resolve(FILE_NAME);
        try {
            Files.createDirectories(outputDir);
            Files.writeString(file, render(result), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AllureComparisonException("Failed to write HTML report " + file, e);
        }
        log.info("HTML report written: {}", file.toAbsolutePath());
        return file;
    }

    String render(ComparisonResult r) {
        StringBuilder html = new StringBuilder(16_384);
        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">")
                .append("<title>Allure Execution Comparison Report</title><style>").append(STYLE)
                .append("</style><script>").append(FILTER_SCRIPT).append("</script></head><body>")
                .append("<h1>Allure Execution Comparison Report</h1>")
                .append("<div class=\"meta\">Generated ").append(LocalDateTime.now().format(TIMESTAMP)).append("</div>");

        appendSummary(html, r);
        appendStatistics(html, r);
        appendFailures(html, r.currentFailures());
        appendSimpleTable(html, "New Tests", r.byType(ChangeType.NEW_TEST), true);
        appendSimpleTable(html, "Removed Tests", r.byType(ChangeType.REMOVED_TEST), false);
        appendDetails(html, r);

        return html.append("</body></html>").toString();
    }

    private static void appendSummary(StringBuilder html, ComparisonResult r) {
        html.append("<h2>Execution Summary</h2><div class=\"cards\">");
        card(html, "Previous Run Total", String.valueOf(r.previousTotal()), "");
        card(html, "Current Run Total", String.valueOf(r.currentTotal()), "");
        card(html, "Pass Rate Previous", pct(r.passRatePrevious()), "");
        card(html, "Pass Rate Current", pct(r.passRateCurrent()), "");
        card(html, "Pass Rate Change", signedPoints(r.passRateDelta()), r.trend().name());
        card(html, "Trend", r.trend().name(), r.trend().name());
        card(html, "Regression Rate", pct(r.regressionPercentage()), r.regressionCount() > 0 ? "REGRESSED" : "");
        card(html, "Improvement Rate", pct(r.improvementPercentage()), r.fixCount() > 0 ? "IMPROVED" : "");
        html.append("</div>");
    }

    private static void appendStatistics(StringBuilder html, ComparisonResult r) {
        html.append("<h2>Status Change Statistics</h2><table><thead><tr><th>Change Type</th><th>Count</th></tr></thead><tbody>");
        r.countsByType().forEach((type, count) -> html.append("<tr class=\"").append(rowClass(type)).append("\"><td>")
                .append(escape(type.label())).append("</td><td>").append(count).append("</td></tr>"));
        html.append("</tbody></table>");
    }

    private static void appendFailures(StringBuilder html, List<TestComparison> failures) {
        html.append("<h2>Current Run Failures (").append(failures.size()).append(")</h2>");
        if (failures.isEmpty()) {
            html.append("<p class=\"empty\">None</p>");
            return;
        }
        html.append("<table><thead><tr><th>Test Name</th><th>Status</th><th>Change Type</th>")
                .append("<th>Failure Details</th></tr></thead><tbody>");
        for (TestComparison c : failures) {
            html.append("<tr class=\"").append(rowClass(c.changeType())).append("\"><td>").append(escape(c.testName()))
                    .append("</td>").append(statusCell(c.currentStatus()))
                    .append("<td>").append(escape(c.changeType().label())).append("</td><td>");
            String message = c.failureMessage();
            html.append("<pre class=\"msg\">")
                    .append(escape(message == null || message.isBlank() ? "(no failure message recorded)" : message))
                    .append("</pre>");
            if (c.failureTrace() != null && !c.failureTrace().isBlank()) {
                html.append("<details><summary>Stack trace</summary><pre class=\"log\">")
                        .append(escape(TextUtils.headLines(c.failureTrace(), TRACE_MAX_LINES))).append("</pre></details>");
            }
            if (c.consoleLog() != null && !c.consoleLog().isBlank()) {
                html.append("<details><summary>Console log (").append(TextUtils.lineCount(c.consoleLog()))
                        .append(" lines)</summary><pre class=\"log\">").append(escape(c.consoleLog()))
                        .append("</pre></details>");
            }
            html.append("</td></tr>");
        }
        html.append("</tbody></table>");
    }

    private static void appendSimpleTable(StringBuilder html, String title, List<TestComparison> rows, boolean useCurrent) {
        html.append("<h2>").append(title).append(" (").append(rows.size()).append(")</h2>");
        if (rows.isEmpty()) {
            html.append("<p class=\"empty\">None</p>");
            return;
        }
        html.append("<table><thead><tr><th>Test Name</th><th>History ID</th><th>")
                .append(useCurrent ? "Current Status" : "Previous Status").append("</th></tr></thead><tbody>");
        for (TestComparison c : rows) {
            TestStatus status = useCurrent ? c.currentStatus() : c.previousStatus();
            html.append("<tr><td>").append(escape(c.testName())).append("</td><td class=\"id\">")
                    .append(escape(c.historyId())).append("</td>").append(statusCell(status)).append("</tr>");
        }
        html.append("</tbody></table>");
    }

    private static void appendDetails(StringBuilder html, ComparisonResult r) {
        html.append("<h2>Detailed Comparison (").append(r.comparisons().size()).append(")</h2>")
                .append("<input type=\"search\" placeholder=\"Filter by name, status or change type\" oninput=\"filterRows(this.value)\">")
                .append("<table id=\"details\"><thead><tr><th>Test Name</th><th>History ID</th><th>Previous Status</th>")
                .append("<th>Current Status</th><th>Change Type</th><th>Failure Message</th></tr></thead><tbody>");
        for (TestComparison c : r.comparisons()) {
            html.append("<tr class=\"").append(rowClass(c.changeType())).append("\"><td>").append(escape(c.testName()))
                    .append("</td><td class=\"id\">").append(escape(c.historyId())).append("</td>")
                    .append(statusCell(c.previousStatus())).append(statusCell(c.currentStatus()))
                    .append("<td>").append(escape(c.changeType().label())).append("</td><td>")
                    .append(escape(TextUtils.firstLine(c.failureMessage(), DETAIL_MESSAGE_MAX_CHARS)))
                    .append("</td></tr>");
        }
        html.append("</tbody></table>");
    }

    private static void card(StringBuilder html, String key, String value, String valueClass) {
        html.append("<div class=\"card\"><div class=\"k\">").append(escape(key)).append("</div><div class=\"v ")
                .append(valueClass).append("\">").append(escape(value)).append("</div></div>");
    }

    private static String statusCell(TestStatus status) {
        if (status == null) {
            return "<td></td>";
        }
        return "<td class=\"" + status.name().toLowerCase(Locale.ROOT) + "\">" + status.label() + "</td>";
    }

    private static String rowClass(ChangeType type) {
        if (type.isRegression()) {
            return "regression";
        }
        return type.isFix() ? "fix" : "";
    }

    private static String pct(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }

    private static String signedPoints(double value) {
        return String.format(Locale.ROOT, "%+.1f pp", value);
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (char ch : value.toCharArray()) {
            switch (ch) {
                case '&' -> out.append("&");
                case '<' -> out.append("<");
                case '>' -> out.append(">");
                case '"' -> out.append("\"");
                case '\'' -> out.append("'");
                default -> out.append(ch);
            }
        }
        return out.toString();
    }
}