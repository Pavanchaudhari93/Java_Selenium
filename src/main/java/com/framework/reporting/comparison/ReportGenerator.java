package com.framework.reporting.comparison;

import java.nio.file.Path;

/**
 * Writes a {@link ComparisonResult} to a file in some output format.
 */
public interface ReportGenerator {

    /**
     * @param outputDir directory to write into; created if missing
     * @return path of the generated report
     * @throws AllureComparisonException if the report cannot be written
     */
    Path generate(ComparisonResult result, Path outputDir);
}