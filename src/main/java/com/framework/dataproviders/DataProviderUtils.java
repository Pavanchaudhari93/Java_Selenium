package com.framework.dataproviders;

import com.framework.config.ConfigReader;
import com.framework.model.User;
import com.framework.utils.CsvUtils;
import com.framework.utils.ExcelUtils;
import com.framework.utils.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.DataProvider;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;

/**
 * Central {@link DataProvider} factory.
 * <p>
 * Exposes Excel, CSV and JSON backed providers:
 * <ul>
 *   <li>{@code excelData} - rows from a configured .xlsx sheet (parallel).</li>
 *   <li>{@code csvData} - rows from a configured CSV file (header skipped).</li>
 *   <li>{@code jsonData} - JSON array mapped to {@link User} POJOs (parallel).</li>
 * </ul>
 * File locations come from config, so no magic strings live in tests.
 */
public final class DataProviderUtils {

    private static final Logger LOG = LogManager.getLogger(DataProviderUtils.class);

    private DataProviderUtils() {
        // utility class - no instantiation
    }

    /**
     * Excel-backed provider. Reads {@code testdata.excel.file} /
     * {@code testdata.excel.sheet} from config.
     *
     * @param method the consuming test method (used only for logging)
     * @return Object[][] of data rows
     */
    @DataProvider(name = "excelData", parallel = true)
    public static Object[][] getExcelData(Method method) {
        String file = resolvePath(ConfigReader.get("testdata.excel.file", "testdata/testdata.xlsx"));
        String sheet = ConfigReader.get("testdata.excel.sheet", "Sheet1");
        Object[][] data = ExcelUtils.getDataFromExcel(file, sheet);
        LOG.info("Excel data provider returned [{}] rows for test [{}]", data.length, method.getName());
        return data;
    }

    /**
     * CSV-backed provider. Reads {@code testdata.csv.file} from config,
     * skipping the header row.
     *
     * @param method the consuming test method (used only for logging)
     * @return Object[][] of data rows
     */
    @DataProvider(name = "csvData")
    public static Object[][] getCsvData(Method method) {
        String file = resolvePath(ConfigReader.get("testdata.csv.file", "testdata/users.csv"));
        List<String[]> rows = CsvUtils.readAll(file, true);
        Object[][] data = new Object[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            data[i] = rows.get(i);
        }
        LOG.info("CSV data provider returned [{}] rows for test [{}]", data.length, method.getName());
        return data;
    }

    /**
     * JSON-backed provider. Reads {@code testdata.json.file} and maps the JSON
     * array onto {@link User} POJOs.
     *
     * @param method the consuming test method (used only for logging)
     * @return Object[][] where each row contains a single {@link User}
     */
    @DataProvider(name = "jsonData", parallel = true)
    public static Object[][] getJsonData(Method method) {
        String file = resolvePath(ConfigReader.get("testdata.json.file", "testdata/users.json"));
        User[] users = JsonUtils.readJsonFile(file, User[].class);
        Object[][] data = new Object[users.length][];
        for (int i = 0; i < users.length; i++) {
            data[i] = new Object[]{users[i]};
        }
        LOG.info("JSON data provider returned [{}] rows for test [{}]", data.length, method.getName());
        return data;
    }

    /**
     * Resolves a relative testdata path against the working directory first,
     * then against the classpath (test resources are copied to target/test-classes).
     *
     * @param relativePath relative path e.g. {@code testdata/users.csv}
     * @return absolute path usable by {@link File}
     */
    private static String resolvePath(String relativePath) {
        File direct = new File(relativePath);
        if (direct.exists()) {
            return direct.getAbsolutePath();
        }
        URL resource = DataProviderUtils.class.getClassLoader().getResource(relativePath);
        if (resource != null) {
            try {
                return new File(resource.toURI()).getAbsolutePath();
            } catch (java.net.URISyntaxException e) {
                LOG.warn("Could not resolve resource URI for [{}], using encoded path", relativePath);
                return new File(resource.getFile()).getAbsolutePath();
            }
        }
        LOG.warn("Could not resolve testdata path [{}], returning as-is", relativePath);
        return relativePath;
    }
}
