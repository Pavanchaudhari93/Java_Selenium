---
name: add-page-object
description: Create or modify a Selenium Page Object in this framework (extends BasePage, @FindBy + PageFactory, wrapped actions). Use when adding locators/actions for a new or changed UI screen.
---

# Adding / editing a Page Object

Location: `src/main/java/com/framework/pages/`. See `LoginPage.java` and `HomePage.java`
for the canonical style.

## Rules

1. **Extend `BasePage`** and forward the constructor:
   ```java
   public class SearchPage extends BasePage {
       public SearchPage(WebDriver driver) { super(driver); }
   }
   ```
   `BasePage`'s constructor already calls `PageFactory.initElements(driver, this)` and
   builds the `WebDriverWait` from `timeout.explicit` config - don't duplicate that.

2. **Declare elements with `@FindBy`, keep them `private`.** Never expose a raw
   `WebElement` through a public getter - only expose behavior (methods), so all
   waiting/logging goes through `BasePage`.

3. **Only use `BasePage`'s protected action methods** - never call
   `element.click()` / `.sendKeys()` / `.getText()` directly. The wrappers give you,
   for free: explicit wait (visible/clickable), Log4j2 debug logging, and on failure a
   screenshot attached to Allure plus a rethrown `IllegalStateException`
   (`handleActionFailure`). Available wrappers:
   - `click(WebElement|By)`
   - `sendKeys(WebElement, String)` / `clearAndType(...)`
   - `getText(WebElement)`, `isDisplayed(WebElement)`, `isElementPresent(By)`
   - `selectDropdownByText/Value(WebElement, String)`
   - `waitForElementVisible/Clickable(WebElement|By)`, `waitForElementToDisappear(By)`
   - `scrollToElement(WebElement)`, `executeJS(String, Object...)`
   - `switchToFrame(WebElement)`, `switchToDefaultContent()`, `switchToWindow(String urlFragment)`
   - `getCurrentUrl()`, `getTitle()`

4. **Fluent / navigation return pattern.** An action that stays on the same page
   returns `this`; an action that navigates returns the *next* page object
   (constructed with the same `driver`):
   ```java
   public HomePage login(String username, String password) {
       enterUsername(username);
       enterPassword(password);
       clickLogin();
       return new HomePage(driver);
   }
   ```

5. **Annotate meaningful public actions with Allure `@Step("...")`**, using
   `{paramName}` placeholders that match the method's parameter names, e.g.
   `@Step("Enter username: {username}")` on `enterUsername(String username)`. Simple
   boolean/query getters (`isXDisplayed`) don't need it; getters that wait+read text
   should still get one (see `HomePage.getWelcomeMessage`).

6. **Never hardcode URLs or other environment-specific values.** Pull them from
   `ConfigReader.get("app.xxxUrl")` etc., as `LoginPage.open()` does.

## Template

```java
package com.framework.pages;

import com.framework.base.BasePage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class SearchPage extends BasePage {

    @FindBy(id = "search-input")
    private WebElement searchInput;

    @FindBy(id = "search-button")
    private WebElement searchButton;

    @FindBy(css = ".no-results")
    private WebElement noResultsBanner;

    public SearchPage(WebDriver driver) {
        super(driver);
    }

    @Step("Search for: {term}")
    public SearchResultsPage search(String term) {
        sendKeys(searchInput, term);
        click(searchButton);
        return new SearchResultsPage(driver);
    }

    public boolean isNoResultsBannerDisplayed() {
        return isDisplayed(noResultsBanner);
    }
}
```

## Checklist

- [ ] Extends `BasePage`, constructor just calls `super(driver)`
- [ ] All elements `private`, declared with `@FindBy`
- [ ] Only `BasePage` wrapper methods used for interaction (no raw `WebElement` calls)
- [ ] Navigation methods return the next page object; same-page actions return `this` or void
- [ ] No hardcoded URLs/timeouts - via `ConfigReader`
- [ ] `@Step` on non-trivial public actions
