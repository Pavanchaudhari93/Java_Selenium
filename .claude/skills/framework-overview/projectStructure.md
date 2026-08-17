# Project Structure

Java 17 + Maven + TestNG automation framework: Selenium (Page Object Model),
REST-assured API testing, multi-DB validation, OWASP ZAP DAST, Allure reporting,
Log4j2 logging. Root package: `com.framework`.

```
Java_Selenium/
├── pom.xml                          # Maven build: deps, Java 17, Surefire/Allure plugins
├── testng.xml                       # Suite definition: listeners + one <test> block per test type
├── README.md                        # Setup, run commands, conventions, troubleshooting
├── projectStructure.md              # This file
│
├── .claude/
│   └── skills/                      # Claude Code skills teaching this framework's conventions
│       ├── framework-overview/SKILL.md      # Architecture map - load first
│       ├── add-page-object/SKILL.md         # How to add a Page Object
│       ├── add-ui-test/SKILL.md             # How to add a UI test
│       ├── add-api-test/SKILL.md            # How to add an API test
│       ├── add-db-validation/SKILL.md       # How to add DB checks / new DB vendor
│       └── add-security-test/SKILL.md       # How to add a ZAP DAST test
│
├── src/main/java/com/framework/
│   ├── base/
│   │   ├── BaseTest.java            # TestNG lifecycle: suite config load, driver init/teardown, screenshots
│   │   ├── BasePage.java            # Page Object superclass: waits, click/type/read, JS, frames/windows
│   │   └── DriverManager.java       # ThreadLocal WebDriver factory (local Chrome/Firefox/Edge or Grid)
│   │
│   ├── config/
│   │   └── ConfigReader.java        # Layered config: system props > config-<env> > config.properties
│   │
│   ├── pages/                       # Page Object Model classes (extend BasePage)
│   │   ├── LoginPage.java
│   │   └── HomePage.java
│   │
│   ├── api/
│   │   ├── ApiBase.java             # RequestSpecification factory (baseUri/basePath/headers/filters)
│   │   ├── ApiHelper.java           # GET/POST/PUT/PATCH/DELETE wrappers over RestAssured
│   │   └── ApiResponseValidator.java# Status code / JSON schema / field / response-time assertions
│   │
│   ├── db/
│   │   ├── DBType.java              # Enum: MYSQL, ORACLE, POSTGRESQL, MSSQL, MONGODB
│   │   ├── DBConfig.java            # Connection parameters holder (host/port/db/user/pass/URI/extra params)
│   │   ├── IDatabaseClient.java     # Common contract: connect/executeQuery/executeUpdate/disconnect
│   │   ├── DatabaseClientFactory.java # Factory: DBType -> IDatabaseClient (JDBC clients share one impl)
│   │   ├── JdbcDatabaseClient.java  # Generic JDBC executor, delegates URL/driver to a strategy
│   │   ├── MongoDatabaseClient.java # MongoDB-specific client ("collection|{filterJson}" query syntax)
│   │   ├── DBUtils.java             # ThreadLocal facade tests call: connect/getRecords/update/close
│   │   └── strategy/
│   │       ├── IDBConnectionStrategy.java         # Strategy contract: driver class + JDBC URL builder
│   │       ├── AbstractJdbcConnectionStrategy.java# Shared connection-opening logic
│   │       ├── MySQLConnectionStrategy.java
│   │       ├── OracleConnectionStrategy.java
│   │       ├── PostgreSQLConnectionStrategy.java
│   │       └── MSSQLConnectionStrategy.java
│   │
│   ├── security/
│   │   └── ZapHelper.java           # OWASP ZAP client wrapper: spider, active scan, alerts, HTML report
│   │
│   ├── dataproviders/
│   │   └── DataProviderUtils.java   # @DataProvider factory: excelData, csvData, jsonData
│   │
│   ├── listeners/                   # Registered in testng.xml <listeners>
│   │   ├── TestListener.java        # ITestListener: pass/fail/skip logging + failure screenshot
│   │   ├── RetryListener.java       # IAnnotationTransformer: auto-attaches RetryAnalyzer to every @Test
│   │   └── AllureListener.java      # Extends AllureTestNg; writes environment.properties for the report
│   │
│   ├── assertions/
│   │   └── SoftAssertHelper.java    # ThreadLocal SoftAssert wrapper w/ logging + Allure steps
│   │
│   ├── utils/
│   │   ├── WaitUtils.java           # Static explicit-wait helpers (visibility, clickability, page load, ...)
│   │   ├── ExtentLogger.java        # Log4j2 + Allure attachment helper (steps, text/json/screenshot/html)
│   │   ├── ScreenshotUtils.java     # Captures PNG screenshot bytes from a WebDriver
│   │   ├── RetryAnalyzer.java       # IRetryAnalyzer: retries per retry.count config, thread-safe counters
│   │   ├── ExcelUtils.java          # Apache POI .xlsx reader for data providers
│   │   ├── CsvUtils.java            # OpenCSV reader for data providers
│   │   ├── JsonUtils.java           # Jackson JSON file <-> POJO mapping
│   │   └── SystemInfoUtils.java     # Environment/system info helpers
│   │
│   └── model/
│       └── User.java                # POJO for JSON test data / API payloads (Jackson @JsonProperty)
│
├── src/main/resources/config/
│   ├── config.properties            # Base/default config (browser, timeouts, app/api URLs, db.*, security.zap.*)
│   ├── config-qa.properties         # QA environment overrides (-Denv=qa, default)
│   └── config-prod.properties       # Prod environment overrides (-Denv=prod)
│
├── src/test/java/com/framework/tests/
│   ├── ui/
│   │   └── LoginTest.java           # Excel-driven login test; group "ui"
│   ├── api/
│   │   └── UserApiTest.java         # CRUD tests against jsonplaceholder; group "api"
│   └── security/
│       └── SecurityTest.java        # ZAP spider + active scan; group "security"; skips if ZAP unreachable
│       # (no db/ subpackage yet - add one + a testng.xml <test> block for the first DB test)
│
├── src/test/resources/
│   ├── log4j2.xml                   # Console + rolling file (logs/automation.log) logging config
│   └── testdata/
│       ├── schemas/users-schema.json# JSON schema used by ApiResponseValidator.validateSchema
│       ├── testdata.xlsx            # Excel-backed data provider source (excelData)
│       ├── users.csv                # CSV-backed data provider source (csvData)
│       └── users.json               # JSON-backed data provider source (jsonData -> User[])
│
├── logs/                            # Runtime Log4j2 output (automation.log, rolled/gzipped) - generated
└── target/, test-output/            # Maven build output, Surefire/TestNG reports - generated, not committed
```

## Test-type -> group -> testng.xml mapping

| Test package | Required `@Test` group | `testng.xml` `<test>` block | Browser started? |
|---|---|---|---|
| `tests.ui` | `ui` | `UI Tests` | Yes (`BaseTest.isUiTest` default) |
| `tests.api` | `api` | `API Tests` | No |
| `tests.security` | `security` | `Security Tests` | No |
| `tests.db` (not yet created) | `db` | *(add a new block)* | No |

A test class is only executed by the suite if it is **both** annotated with the
matching group **and** listed under that block's `<classes>` in `testng.xml`.

## Key design patterns in this codebase

- **Page Object Model + Page Factory** - `pages/*` extend `BasePage`, `@FindBy` fields
  initialized via `PageFactory.initElements`.
- **Factory + Strategy** - `db.DatabaseClientFactory` (Factory) selects an
  `IDatabaseClient`; JDBC vendors share `JdbcDatabaseClient` but plug in a different
  `strategy.IDBConnectionStrategy` (Strategy) for driver/URL building.
- **Facade** - `DBUtils` hides the Factory/Strategy/client machinery behind
  `connect/getRecords/update/close`.
- **ThreadLocal isolation** for parallel execution - `DriverManager`,
  `SoftAssertHelper`, `DBUtils` each keep one instance per thread.
- **Layered configuration** - `ConfigReader` merges system properties, per-env
  properties, and base properties, in that precedence order.

See `.claude/skills/framework-overview/SKILL.md` for the full narrative version of
this map, and the other `.claude/skills/*` files for step-by-step guidance on adding
each kind of test/component.
