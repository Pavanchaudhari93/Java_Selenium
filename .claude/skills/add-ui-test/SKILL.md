---
name: add-ui-test
description: Write or modify a UI test class in this framework (extends BaseTest, TestNG, page objects, Excel/CSV/JSON data providers, hard/soft assertions, Allure annotations). Use when adding or changing browser-driven test coverage.
---

# Adding / editing a UI test

Location: `src/test/java/com/framework/tests/ui/`. See `LoginTest.java` for the
canonical style. Pair with **add-page-object** if the screen under test doesn't have
a Page Object yet.

## Rules

1. **Extend `BaseTest`.** It gives you `getDriver()`, automatic WebDriver init/teardown,
   config loading, and failure screenshots. Never call `DriverManager` or construct a
   `WebDriver` yourself.

2. **Tag the test `groups = {"ui"}`.** `BaseTest.isUiTest` already treats anything
   without `api`/`security`/`db` as a UI test, but `testng.xml`'s `<test name="UI Tests">`
   block filters on the `ui` group, so it must be set explicitly or the test won't run
   in the suite.

3. **Register the class in `testng.xml`** under `<test name="UI Tests"><classes>` -
   a class that compiles but isn't listed there never executes.

4. **Use Allure grouping annotations**: `@Feature("...")` at class level,
   `@Story("...")` per test method (see `LoginTest`).

5. **Data providers** (`com.framework.dataproviders.DataProviderUtils`):
   ```java
   @Test(dataProvider = "excelData", dataProviderClass = DataProviderUtils.class, groups = {"ui"})
   ```
   - `excelData` - rows from `testdata.excel.file` / `testdata.excel.sheet` (config).
   - `csvData` - rows from `testdata.csv.file`, header row skipped.
   - `jsonData` - JSON array mapped onto a POJO (see `model.User`).
   Test data files live in `src/test/resources/testdata/`. Don't hardcode credentials/
   test rows inline when a data provider fits.

6. **Assertions**:
   - Hard (`org.testng.Assert`) for the one critical checkpoint that should abort the
     test immediately on failure.
   - Soft (`SoftAssertHelper`) for secondary checks you want to collect together -
     **you must call `SoftAssertHelper.assertAll()` yourself** at the end of each
     branch, or the collected failures are silently dropped. Don't call `reset()` -
     `BaseTest.tearDown` does that automatically after every test.

7. Interact with the page only through its Page Object's public methods - never reach
   into `getDriver()` to call raw Selenium in a test unless there's truly no Page Object
   concept for it.

## Template

```java
package com.framework.tests.ui;

import com.framework.assertions.SoftAssertHelper;
import com.framework.base.BaseTest;
import com.framework.dataproviders.DataProviderUtils;
import com.framework.pages.SearchPage;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

@Feature("Search")
public class SearchTest extends BaseTest {

    @Test(dataProvider = "csvData", dataProviderClass = DataProviderUtils.class, groups = {"ui"})
    @Story("Search returns matching results")
    public void testSearch(String term, String expectedFirstResult) {
        SearchPage searchPage = new SearchPage(getDriver());
        var resultsPage = searchPage.search(term);

        Assert.assertTrue(resultsPage.hasResults(), "Expected at least one result for: " + term);

        SoftAssertHelper.assertEquals(resultsPage.getFirstResultTitle(), expectedFirstResult,
                "First result title should match");
        SoftAssertHelper.assertAll();
    }
}
```

Then add `<class name="com.framework.tests.ui.SearchTest"/>` under the `UI Tests`
`<test>` block in `testng.xml`.

## Checklist

- [ ] Extends `BaseTest`; no direct `DriverManager`/`WebDriver` usage
- [ ] `@Test(groups = {"ui"}, ...)`
- [ ] Class added to `testng.xml` -> `UI Tests` -> `<classes>`
- [ ] `@Feature`/`@Story` present
- [ ] Every `SoftAssertHelper` block ends with `assertAll()`
- [ ] All page interaction goes through a Page Object
