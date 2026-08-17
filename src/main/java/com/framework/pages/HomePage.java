package com.framework.pages;

import com.framework.base.BasePage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Home page object shown after a successful login
 * (the-internet.herokuapp.com/secure).
 */
public class HomePage extends BasePage {

    @FindBy(css = ".flash.success")
    private WebElement successMessage;

    @FindBy(css = "a[href='/logout']")
    private WebElement logoutLink;

    public HomePage(WebDriver driver) {
        super(driver);
    }

    /**
     * @return {@code true} when the welcome banner is displayed
     */
    public boolean isWelcomeMessageDisplayed() {
        return isDisplayed(successMessage);
    }

    /**
     * @return the welcome banner text, e.g.
     * "You logged into a secure area!"
     */
    @Step("Read welcome message")
    public String getWelcomeMessage() {
        waitForElementVisible(successMessage);
        return getText(successMessage);
    }

    /**
     * Logs out and returns to the login page.
     *
     * @return the login page object
     */
    @Step("Logout")
    public LoginPage logout() {
        click(logoutLink);
        return new LoginPage(driver);
    }
}
