package com.framework.base;

import com.framework.config.ConfigReader;
import com.framework.utils.ExtentLogger;
import com.framework.utils.ScreenshotUtils;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Set;

/**
 * Base class for all Page Objects.
 * <p>
 * Provides common, synchronized Selenium actions (click, type, read text,
 * dropdowns, frames, windows, JS execution) backed by explicit
 * {@link WebDriverWait}. Every action is logged via Log4j2 and, on failure,
 * a screenshot is attached to the Allure report before the exception is
 * rethrown. Subclasses use {@code @FindBy} fields and {@link org.openqa.selenium.support.PageFactory}.
 */
public abstract class BasePage {

    private static final Logger LOG = LogManager.getLogger(BasePage.class);

    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final JavascriptExecutor js;
    protected final Actions actions;

    /**
     * Initializes the page object and PageFactory elements.
     *
     * @param driver the WebDriver instance for the current thread
     */
    protected BasePage(WebDriver driver) {
        this.driver = driver;
        long timeout = ConfigReader.getInt("timeout.explicit", 30);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
        this.js = (JavascriptExecutor) driver;
        this.actions = new Actions(driver);
        org.openqa.selenium.support.PageFactory.initElements(driver, this);
    }

    // ------------------------------------------------------------------
    // Clicks
    // ------------------------------------------------------------------

    /**
     * Waits for the element to be clickable and clicks it.
     *
     * @param element target element
     */
    @Step("Click on element")
    protected void click(WebElement element) {
        try {
            waitForElementClickable(element).click();
            LOG.debug("Clicked on element [{}]", describe(element));
        } catch (Exception e) {
            handleActionFailure("Failed to click element", e, element);
        }
    }

    /**
     * Clicks an element located by a {@link By} locator.
     *
     * @param locator By locator
     */
    protected void click(By locator) {
        click(waitForElementVisible(locator));
    }

    // ------------------------------------------------------------------
    // Text input
    // ------------------------------------------------------------------

    /**
     * Clears and types text into an input element.
     *
     * @param element input element
     * @param text    text to type
     */
    @Step("Type text into element")
    protected void sendKeys(WebElement element, String text) {
        try {
            waitForElementVisible(element).clear();
            element.sendKeys(text);
            LOG.debug("Typed text into element [{}]", describe(element));
        } catch (Exception e) {
            handleActionFailure("Failed to type into element", e, element);
        }
    }

    /**
     * Clears a field and types text.
     *
     * @param element input element
     * @param text    text to type
     */
    protected void clearAndType(WebElement element, String text) {
        sendKeys(element, text);
    }

    // ------------------------------------------------------------------
    // Reading state
    // ------------------------------------------------------------------

    /**
     * Returns the visible text of an element.
     *
     * @param element target element
     * @return element text
     */
    protected String getText(WebElement element) {
        try {
            String text = waitForElementVisible(element).getText();
            LOG.debug("Read text from element [{}]: [{}]", describe(element), text);
            return text;
        } catch (Exception e) {
            handleActionFailure("Failed to read text from element", e, element);
            return null; // unreachable, handleActionFailure always throws
        }
    }

    /**
     * @param element target element
     * @return {@code true} if the element is displayed and visible
     */
    protected boolean isDisplayed(WebElement element) {
        try {
            boolean displayed = element.isDisplayed();
            LOG.debug("Element [{}] displayed = {}", describe(element), displayed);
            return displayed;
        } catch (Exception e) {
            LOG.debug("Element [{}] not displayed: {}", describe(element), e.getMessage());
            return false;
        }
    }

    /**
     * @param locator By locator
     * @return {@code true} if at least one matching element is present in the DOM
     */
    protected boolean isElementPresent(By locator) {
        try {
            driver.findElement(locator);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @return current page URL
     */
    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * @return current page title
     */
    protected String getTitle() {
        return driver.getTitle();
    }

    // ------------------------------------------------------------------
    // Dropdowns
    // ------------------------------------------------------------------

    /**
     * Selects a dropdown option by its visible text.
     *
     * @param element dropdown element
     * @param text    visible option text
     */
    protected void selectDropdownByText(WebElement element, String text) {
        try {
            waitForElementVisible(element);
            new Select(element).selectByVisibleText(text);
            LOG.debug("Selected dropdown option [{}]", text);
        } catch (Exception e) {
            handleActionFailure("Failed to select dropdown option by text", e, element);
        }
    }

    /**
     * Selects a dropdown option by its value attribute.
     *
     * @param element dropdown element
     * @param value   option value
     */
    protected void selectDropdownByValue(WebElement element, String value) {
        try {
            waitForElementVisible(element);
            new Select(element).selectByValue(value);
            LOG.debug("Selected dropdown option value [{}]", value);
        } catch (Exception e) {
            handleActionFailure("Failed to select dropdown option by value", e, element);
        }
    }

    // ------------------------------------------------------------------
    // Waiting
    // ------------------------------------------------------------------

    /**
     * Waits until an element is visible.
     *
     * @param locator By locator
     * @return the visible element
     */
    protected WebElement waitForElementVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Waits until an element is visible.
     *
     * @param element element
     * @return the visible element
     */
    protected WebElement waitForElementVisible(WebElement element) {
        return wait.until(ExpectedConditions.visibilityOf(element));
    }

    /**
     * Waits until an element is clickable.
     *
     * @param element element
     * @return the clickable element
     */
    protected WebElement waitForElementClickable(WebElement element) {
        return wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    /**
     * Waits until an element located by {@code locator} disappears.
     *
     * @param locator By locator
     */
    protected void waitForElementToDisappear(By locator) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    // ------------------------------------------------------------------
    // Scrolling & JavaScript
    // ------------------------------------------------------------------

    /**
     * Scrolls the target element into view using JS.
     *
     * @param element element to scroll to
     */
    protected void scrollToElement(WebElement element) {
        try {
            js.executeScript("arguments[0].scrollIntoView(true);", element);
            LOG.debug("Scrolled to element [{}]", describe(element));
        } catch (Exception e) {
            handleActionFailure("Failed to scroll to element", e, element);
        }
    }

    /**
     * Executes a JavaScript snippet on the page.
     *
     * @param script   JS code
     * @param arguments optional script arguments
     * @return JS execution result
     */
    protected Object executeJS(String script, Object... arguments) {
        try {
            return js.executeScript(script, arguments);
        } catch (Exception e) {
            LOG.error("Failed to execute JS script [{}]", script, e);
            throw new IllegalStateException("JS execution failed: " + script, e);
        }
    }

    // ------------------------------------------------------------------
    // Frames & Windows
    // ------------------------------------------------------------------

    /**
     * Switches WebDriver context into the given frame.
     *
     * @param frameElement frame element
     */
    protected void switchToFrame(WebElement frameElement) {
        waitForElementVisible(frameElement);
        driver.switchTo().frame(frameElement);
        LOG.debug("Switched to frame [{}]", describe(frameElement));
    }

    /**
     * Switches back to the default content from any frame.
     */
    protected void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }

    /**
     * Switches to a window whose URL contains the given fragment.
     *
     * @param urlFragment fragment of the target window URL
     */
    protected void switchToWindow(String urlFragment) {
        Set<String> handles = driver.getWindowHandles();
        for (String handle : handles) {
            String windowUrl = driver.switchTo().window(handle).getCurrentUrl();
            if (windowUrl != null && windowUrl.contains(urlFragment)) {
                LOG.debug("Switched to window [{}] containing [{}]", handle, urlFragment);
                return;
            }
        }
        throw new IllegalStateException("No window found containing URL fragment: " + urlFragment);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Logs the failure, attaches a screenshot to Allure and rethrows.
     *
     * @param message failure description
     * @param e       original exception
     * @param element element involved in the action
     */
    protected void handleActionFailure(String message, Exception e, WebElement element) {
        LOG.error("{} | Element: {}", message, describe(element), e);
        try {
            ExtentLogger.attachScreenshot("Action failure - " + message, ScreenshotUtils.captureScreenshot(driver));
        } catch (Exception screenshotError) {
            LOG.warn("Could not attach failure screenshot", screenshotError);
        }
        throw new IllegalStateException(message, e);
    }

    private String describe(WebElement element) {
        try {
            return element.toString().replace("->", "->").trim();
        } catch (Exception e) {
            return "unknown element";
        }
    }
}
