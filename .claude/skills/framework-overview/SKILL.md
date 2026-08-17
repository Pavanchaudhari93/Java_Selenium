---
name: framework-overview
description: Orientation to this repo's Java Selenium/TestNG automation framework - architecture, package layout, config precedence, threading model, and which skill to use for a given change. Load this first for any work in this codebase, before add-page-object/add-ui-test/add-api-test/add-db-validation/add-security-test.
---

# Selenium TestNG Automation Framework - Overview

Java 17 + Maven + TestNG framework: Page Object Model (Selenium), REST-assured API
testing, multi-DB validation (Postgres/MySQL/MSSQL/Oracle/MongoDB), OWASP ZAP DAST,
Allure reporting, Log4j2 logging. Root package: `com.framework`.

## Package map

| Package | Purpose |
|---|---|
| `base` | `BaseTest` (TestNG lifecycle), `BasePage` (Selenium action wrappers), `DriverManager` (ThreadLocal WebDriver) |
| `config` | `ConfigReader` - single entry point for all configuration |
| `pages` | Page Objects (`LoginPage`, `HomePage`, ...) - extend `BasePage` |
| `api` | `ApiBase` (RequestSpec factory), `ApiHelper` (HTTP verbs), `ApiResponseValidator` (assertions) |
| `db` | `DatabaseClientFactory` (Factory) + `strategy/*ConnectionStrategy` (Strategy) + `DBUtils` (ThreadLocal facade) |
| `security` | `ZapHelper` - OWASP ZAP client wrapper |
| `dataproviders` | `DataProviderUtils` - Excel/CSV/JSON `@DataProvider`s |
| `listeners` | `TestListener`, `RetryListener`, `AllureListener` (registered in `testng.xml`) |
| `assertions` | `SoftAssertHelper` - ThreadLocal `SoftAssert` wrapper |
| `utils` | `WaitUtils`, `ExtentLogger`, `ScreenshotUtils`, `RetryAnalyzer`, `ExcelUtils`, `CsvUtils`, `JsonUtils`, `SystemInfoUtils` |
| `model` | POJOs for JSON test data / API payloads (e.g. `User`) |
| `tests/{ui,api,security}` (src/test) | Test classes, one subpackage per test type |

Which skill to use:
- New/changed screen -> **add-page-object**
- New UI test flow -> **add-ui-test**
- New REST endpoint coverage -> **add-api-test**
- New DB check or DB vendor -> **add-db-validation**
- New ZAP/DAST test -> **add-security-test**

## Test lifecycle (`BaseTest`)

Every test class extends `BaseTest`:
- `@BeforeSuite` loads config once (`ConfigReader.loadConfig()`).
- `@BeforeMethod` initializes the WebDriver and navigates to `app.baseUrl` **unless**
  the `@Test` has group `api`, `security`, or `db` (see `BaseTest.isUiTest`).
- `@AfterMethod` screenshots on failure, quits the driver, resets soft asserts.

**Consequence:** any test in a non-`ui` group must be tagged `groups = {"api"}` /
`{"security"}` / `{"db"}`, or it will pointlessly spin up a browser. Never call
`DriverManager` or instantiate a `WebDriver` directly from a test - always go through
`BaseTest.getDriver()`.

## testng.xml wiring

`testng.xml` defines one `<test>` block per test type, each filtering by group and
explicitly listing classes:

```xml
<test name="API Tests">
  <groups><run><include name="api"/></run></groups>
  <classes><class name="com.framework.tests.api.UserApiTest"/></classes>
</test>
```

**A new test class is invisible to the suite until it is added to the matching
`<classes>` list** (or a new `<test>` block, e.g. there is currently no `db` block -
add one when the first DB test is written). Compiling is not enough.

Listeners (`TestListener`, `RetryListener`, `AllureListener`) are registered once at
the top of `testng.xml` and apply to every test automatically.

## Configuration (`ConfigReader`)

Precedence, highest first: JVM system property (`-Dkey=value`) > `config-<env>.properties`
> `config.properties`. Files live in `src/main/resources/config/`, env selected via
`-Denv=qa|prod|staging|dev` (default `qa`).

- Never hardcode URLs, timeouts, credentials, or endpoints in test/page code.
- Add a default in `config.properties`, override per environment in `config-<env>.properties`.
- Read via `ConfigReader.get(key)` / `get(key, default)` / `getInt(...)` / `getBoolean(...)`.

## Threading model

`testng.xml` runs `parallel="methods"` with `thread-count="4"`. Everything shared
across a test run is `ThreadLocal`:
- `DriverManager` (WebDriver per thread)
- `SoftAssertHelper` (SoftAssert per thread)
- `DBUtils` (DB client per thread)

Never cache a `WebDriver`, `SoftAssert`, or DB client in a static or instance field
yourself - always fetch it through the owning class each time, or thread-safety breaks
silently under parallel execution.

## Assertions

- Hard: `org.testng.Assert.*` - stops the test immediately. Use for the primary checkpoint.
- Soft: `SoftAssertHelper.*` - collects failures; **you must call
  `SoftAssertHelper.assertAll()` yourself** at the end of the test/branch or the
  failures are silently discarded. `BaseTest.tearDown` only resets state for the next
  test, it does not flush assertions.

## Reporting & logging

- Log4j2: `LOG = LogManager.getLogger(ThisClass.class)`; console + rolling file at
  `logs/automation.log` (config: `src/test/resources/log4j2.xml`).
- Allure: annotate meaningful public methods with `@Step("...")` (supports
  `{paramName}` placeholders bound to method arguments). Use `ExtentLogger` for
  ad-hoc steps/attachments (`step`, `attachText`, `attachJson`, `attachScreenshot`,
  `attachHtml`) instead of raw `System.out`.
- Screenshots on failure are automatic (`BasePage.handleActionFailure`, `BaseTest.tearDown`,
  `TestListener.onTestFailure`) - don't add your own failure-screenshot logic in tests.

## Retry

Controlled purely by `retry.count` in config (default `0` = disabled). `RetryListener`
(an `IAnnotationTransformer`) attaches `RetryAnalyzer` to every `@Test` automatically -
never add `retryAnalyzer = ...` manually on a `@Test` annotation.

## Running

```bash
mvn clean test                                   # full suite
mvn clean test -Denv=qa                          # environment
mvn clean test -Dbrowser=firefox                 # browser
mvn clean test -Dbrowser=chrome -Dheadless=true   # headless
mvn clean test -Dgroups=ui|api|security          # subset
mvn clean test -Dexecution.mode=grid -Dgrid.url=http://localhost:4444/wd/hub
mvn allure:serve                                 # view report
```
