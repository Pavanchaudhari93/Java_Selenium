# Selenium TestNG Automation Framework

Production-grade **Java Selenium WebDriver** test automation framework built with
**Maven**, **TestNG**, Page Object Model + Page Factory, Allure reporting, Log4j2,
REST-assured API testing, multi-database connectivity (PostgreSQL, MySQL, MSSQL,
Oracle, MongoDB) and OWASP ZAP DAST scanning.

## Tech Stack

| Area          | Technology                                                        |
|---------------|-------------------------------------------------------------------|
| Language      | Java 17                                                           |
| Build         | Maven                                                             |
| UI Automation | Selenium WebDriver 4.x + WebDriverManager (driver binaries)       |
| Test Runner   | TestNG 7.x (parallel methods supported)                           |
| Reporting     | Allure (testng + rest-assured plugins), screenshots on failure    |
| Logging       | Log4j2 (console + rolling file)                                   |
| API Testing   | REST-assured + JSON Schema Validator + Allure-RestAssured filter  |
| JSON          | Jackson + Gson                                                    |
| Test Data     | Apache POI (Excel), OpenCSV, JSON files                           |
| Databases     | PostgreSQL / MySQL / MSSQL / Oracle (JDBC) + MongoDB Java Driver  |
| Security      | OWASP ZAP Java API (spider + active scan + report)                |
| Config        | `config.properties` per environment (dev/qa/staging/prod)         |

## Project Structure

```
├── pom.xml
├── testng.xml
├── src/main/java/com/framework/
│   ├── base/            # BaseTest, BasePage, DriverManager (ThreadLocal)
│   ├── config/          # ConfigReader + config.properties (dev/qa/prod)
│   ├── pages/           # LoginPage, HomePage (Page Object Model)
│   ├── utils/           # WaitUtils, ExcelUtils, CsvUtils, ScreenshotUtils,
│   │                    # ExtentLogger, RetryAnalyzer, JsonUtils
│   ├── dataproviders/   # DataProviderUtils (Excel + CSV + JSON)
│   ├── db/              # DBConnectionFactory + connectors for 5 DBs
│   ├── api/             # ApiBase, ApiHelper, ApiResponseValidator
│   ├── security/        # ZapHelper
│   ├── listeners/       # TestListener, RetryListener, AllureListener
│   ├── assertions/      # SoftAssertHelper
│   └── model/           # POJOs (e.g. User)
├── src/test/java/com/framework/tests/
│   ├── ui/              # LoginTest
│   ├── api/             # UserApiTest
│   └── security/        # SecurityTest
└── src/test/resources/
    ├── testdata/        # Excel/CSV/JSON samples + JSON schemas
    └── log4j2.xml
```

## Prerequisites

- **JDK 17** (code targets Java 17; override via `-Dmaven.compiler.release` for older JDKs)
- **Maven 3.8+**
- A browser (Chrome/Firefox/Edge) - drivers are auto-managed by WebDriverManager
- (Optional) OWASP ZAP running in daemon mode for security tests
- (Optional) DB instances for DB validation tests

## How to Run

```bash
# Full suite (UI + API + Security)
mvn clean test

# Select environment (qa/prod/staging/dev)
mvn clean test -Denv=qa

# Select browser
mvn clean test -Dbrowser=chrome
mvn clean test -Dbrowser=firefox
mvn clean test -Dbrowser=edge

# Headless mode
mvn clean test -Dbrowser=chrome -Dheadless=true

# Selenium Grid / remote execution
mvn clean test -Dexecution.mode=grid -Dgrid.url=http://localhost:4444/wd/hub

# Run only UI tests (override suite selection)
mvn clean test -Dgroups=ui
mvn clean test -Dgroups=api
```

> Note: the `-Dbrowser=...`, `-Denv=...` system properties always override the
> properties files (see ConfigReader precedence).

## Allure Reports

```bash
# Generate the HTML report into target/site/allure-maven-plugin
mvn allure:report

# Serve the report locally in a browser
mvn allure:serve
```

Allure captures test steps (`@Step`), request/response payloads (via the
Allure-RestAssured filter), failure screenshots and environment info
(browser/env/mode written by `AllureListener`).

## Configuration & Environments

Configuration lives in `src/main/resources/config/`:

| File                   | Purpose                                  |
|------------------------|------------------------------------------|
| `config.properties`    | Base/default values                      |
| `config-qa.properties` | QA overrides (use `-Denv=qa`)            |
| `config-prod.properties` | Prod overrides (use `-Denv=prod`)      |

**Precedence (highest first):**
1. JVM system properties: `-Dbrowser=firefox -Dtimeout.explicit=15`
2. `config-<env>.properties`
3. `config.properties`

`ConfigReader` is the single entry point (`ConfigReader.get("key")`,
`ConfigReader.getInt(...)`, `ConfigReader.getBoolean(...)`).

## Adding a New Page Object

1. Extend `BasePage` (it calls `PageFactory.initElements` in its constructor).
2. Declare locators with `@FindBy` and keep selectors in the page class.
3. Use the protected base actions (`click`, `sendKeys`, `getText`, ...) which
   already handle waits, logging and screenshot-on-exception.

```java
public class SearchPage extends BasePage {
    @FindBy(id = "search-input") private WebElement searchInput;
    @FindBy(id = "search-button") private WebElement searchButton;

    public SearchPage(WebDriver driver) { super(driver); }

    public SearchResultsPage search(String term) {
        sendKeys(searchInput, term);
        click(searchButton);
        return new SearchResultsPage(driver);
    }
}
```

## Adding a New Test

Extend `BaseTest` (gives you `getDriver()`, driver lifecycle, config loading
and failure screenshots). UI tests run in group `ui`; mark non-UI tests
(`api`, `security`, `db`) so `BaseTest` skips browser setup.

```java
@Test(dataProvider = "excelData", dataProviderClass = DataProviderUtils.class, groups = {"ui"})
public void myUiTest(String colA, String colB) {
    // page object + hard/soft assertions
}
```

## Assertions

- **Hard assertions** (`org.testng.Assert`) for critical checkpoints.
- **Soft assertions** (`SoftAssertHelper`) to validate multiple fields without
  stopping execution; call `SoftAssertHelper.assertAll()` at the end of the test.

## Adding a New DB Connection

1. Add the driver dependency to `pom.xml` (JDBC URL drivers are already included).
2. Add config keys to `config.properties` (and per-env files).
3. Implement `DatabaseConnector` (extend `AbstractSqlConnector`) or add a MongoDB-style class.
4. Register it in `DBConnectionFactory`.

```java
Connection conn = DBConnectionFactory.getConnection(DBType.POSTGRES);
List<Map<String, Object>> rows = new PostgresConnector().executeQuery("SELECT * FROM users");
// always close connections (try-with-resources)
```

For MongoDB:

```java
MongoDatabase db = DBConnectionFactory.getMongoDatabase();
db.getCollection("users").countDocuments();
```

### DB connection pooling (HikariCP)

The framework ships a simple working JDBC version by default (connections are
opened per call and closed in `finally`/try-with-resources). For production
workloads, uncomment the HikariCP dependency in `pom.xml` and swap
`DriverManager.getConnection(...)` in `AbstractSqlConnector` for a
`HikariDataSource` with a `ConfigReader`-driven pool config.

## Retry Logic

- `retry.count` config controls retries (default `0` = disabled).
- `RetryListener` (IAnnotationTransformer) attaches `RetryAnalyzer` to every
  test automatically; counters are per-method and thread-safe.

## Parallel Execution

`testng.xml` runs with `parallel="methods"` and `thread-count="4"`. Each thread
gets an isolated WebDriver via `DriverManager` (ThreadLocal). Data providers
run in parallel too (`parallel = true`). Change the `parallel`/`thread-count`
attributes to switch modes (`tests`, `classes`, `methods`, `instances`).

## Security Testing (OWASP ZAP)

1. Start ZAP in daemon mode: `zap.sh -daemon -port 8080`.
2. Point `security.zap.targetUrl` at a target app (use a safe/staging URL).
3. Run `mvn clean test -Dgroups=security`.

`ZapHelper` drives spider + active scan, reports alert counts and exports an
HTML report to `target/zap-reports/`. If ZAP is not reachable the test skips
instead of failing.

## Logs

Logs are written to the console and to `logs/automation.log` (rolling, gzipped).
The pattern includes timestamp, thread, class, level and message - essential
for debugging parallel runs. Configuration: `src/test/resources/log4j2.xml`.

## Troubleshooting

- **Drivers**: WebDriverManager downloads matching browser drivers automatically;
  ensure the target browser is installed.
- **Parallel failures**: check thread id in the log line `[t]`; driver state is
  thread-local so no shared state should be touched across tests.
- **Excel data provider errors**: confirm `testdata/testdata.xlsx` exists and the
  sheet name matches `testdata.excel.sheet`.
- **ZAP security test skips**: ZAP isn't reachable - start it or override
  `security.zap.url`.
