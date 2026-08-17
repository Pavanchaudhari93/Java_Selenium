package com.framework.pages;

import com.framework.base.BasePage;
import com.framework.config.ConfigReader;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Login page object (the-internet.herokuapp.com/login).
 * Demonstrates the Page Object Model + Page Factory pattern: elements are
 * declared with {@code @FindBy}, initialized by {@link BasePage}, and all
 * actions are wrapped with explicit waits + logging.
 */
public class LoginPage extends BasePage {

    @FindBy(id = "username")
    private WebElement usernameField;

    @FindBy(id = "password")
    private WebElement passwordField;

    @FindBy(css = "button[type='submit']")
    private WebElement loginButton;

    @FindBy(css = ".flash.error")
    private WebElement errorMessage;

    @FindBy(css = ".flash.success")
    private WebElement successMessage;

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Opens the login page.
     *
     * @return this page object
     */
    @Step("Open login page")
    public LoginPage open() {
        driver.get(ConfigReader.get("app.loginUrl"));
        waitForElementVisible(usernameField);
        return this;
    }

    /**
     * Types a username into the username field.
     *
     * @param username username value
     * @return this page object
     */
    @Step("Enter username: {username}")
    public LoginPage enterUsername(String username) {
        sendKeys(usernameField, username);
        return this;
    }

    /**
     * Types a password into the password field.
     *
     * @param password password value
     * @return this page object
     */
    @Step("Enter password: {password}")
    public LoginPage enterPassword(String password) {
        sendKeys(passwordField, password);
        return this;
    }

    /**
     * Clicks the login button.
     *
     * @return this page object
     */
    @Step("Click login button")
    public LoginPage clickLogin() {
        click(loginButton);
        return this;
    }

    /**
     * Performs a complete login flow.
     *
     * @param username username value
     * @param password password value
     * @return the home page after submitting credentials
     */
    @Step("Login with username: {username}")
    public HomePage login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
        return new HomePage(driver);
    }

    /**
     * @return {@code true} when the error banner is displayed
     */
    public boolean isErrorMessageDisplayed() {
        return isDisplayed(errorMessage);
    }

    /**
     * @return the visible error message text
     */
    @Step("Read error message")
    public String getErrorMessage() {
        return getText(errorMessage);
    }

    /**
     * @return {@code true} when the success banner is displayed
     */
    public boolean isSuccessMessageDisplayed() {
        return isDisplayed(successMessage);
    }

    /**
     * @return the visible success message text
     */
    @Step("Read success message")
    public String getSuccessMessage() {
        return getText(successMessage);
    }
}
