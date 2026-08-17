package com.framework.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Reads Excel (.xlsx) test data files using Apache POI.
 * <p>
 * The first row is treated as the header and skipped; every subsequent row is
 * returned as an {@code Object[]} with values normalized to Strings via
 * {@link DataFormatter}. Thread-safe as every call opens a fresh stream.
 */
public final class ExcelUtils {

    private static final Logger LOG = LogManager.getLogger(ExcelUtils.class);
    private static final DataFormatter FORMATTER = new DataFormatter();

    private ExcelUtils() {
        // utility class - no instantiation
    }

    /**
     * Reads all data rows from an Excel sheet.
     *
     * @param filePath  absolute or classpath-relative path to the .xlsx file
     * @param sheetName name of the sheet to read
     * @return {@code Object[][]} where each row is a test data row (header skipped)
     */
    public static Object[][] getDataFromExcel(String filePath, String sheetName) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException(
                        "Sheet '" + sheetName + "' not found in file: " + filePath);
            }

            int lastRowNum = sheet.getLastRowNum();
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException(
                        "Sheet '" + sheetName + "' has no header row in file: " + filePath);
            }
            int columnCount = headerRow.getLastCellNum();

            Object[][] data = new Object[Math.max(lastRowNum, 0)][columnCount];
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                for (int j = 0; j < columnCount; j++) {
                    Cell cell = row != null ? row.getCell(j) : null;
                    data[i - 1][j] = cell != null ? FORMATTER.formatCellValue(cell) : "";
                }
            }
            LOG.info("Loaded [{}] data rows from Excel [{}] sheet [{}]", data.length, filePath, sheetName);
            return data;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read Excel file: " + filePath, e);
        }
    }
}
