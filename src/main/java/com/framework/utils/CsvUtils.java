package com.framework.utils;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * CSV reader built on OpenCSV.
 * <p>
 * Reads a CSV file into rows of {@code String[]}. Optionally skips the header
 * row. Every call opens a fresh reader, so it is safe for parallel data
 * providers.
 */
public final class CsvUtils {

    private static final Logger LOG = LogManager.getLogger(CsvUtils.class);

    private CsvUtils() {
        // utility class - no instantiation
    }

    /**
     * Reads all CSV rows including the header.
     *
     * @param filePath path to the CSV file
     * @return list of rows, each row an array of field values
     */
    public static List<String[]> readAll(String filePath) {
        return readAll(filePath, false);
    }

    /**
     * Reads all CSV rows, optionally skipping the header.
     *
     * @param filePath     path to the CSV file
     * @param skipHeader   when {@code true}, the first row is omitted
     * @return list of rows, each row an array of field values
     */
    public static List<String[]> readAll(String filePath, boolean skipHeader) {
        try (FileReader reader = new FileReader(filePath)) {
            CSVReaderBuilder builder = new CSVReaderBuilder(reader);
            if (skipHeader) {
                builder = builder.withSkipLines(1);
            }
            try (CSVReader csvReader = builder.build()) {
                List<String[]> rows = csvReader.readAll();
                LOG.info("Loaded [{}] rows from CSV file [{}] (skipHeader={})", rows.size(), filePath, skipHeader);
                return rows;
            }
        } catch (IOException | com.opencsv.exceptions.CsvException e) {
            throw new IllegalStateException("Failed to read CSV file: " + filePath, e);
        }
    }
}
