package com.framework.tests.ui;

import com.framework.assertions.SoftAssertHelper;
import com.framework.base.BaseTest;
import com.framework.dataproviders.DataProviderUtils;
import com.framework.pages.HomePage;
import com.framework.pages.LoginPage;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Login UI tests (the-internet.herokuapp.com).
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>Excel-backed data provider ({@code excelData}).</li>
 *   <li>Hard assertions (org.testng.Assert) for critical checkpoints.</li>
 *   <li>Soft assertions (SoftAssertHelper) for secondary validations.</li>
 * </ul>
 * Excel columns: username | password | expectedStatus (valid/invalid)
 */
@Feature("Login")
public class LoginTest extends BaseTest {

    @Test(dataProvider = "excelData", dataProviderClass = DataProviderUtils.class, groups = {"ui"})
    @Story("Login with Excel-driven credentials")
    public void testLogin(String username, String password, String expectedStatus) {
        LoginPage loginPage = new LoginPage(getDriver()).open();
        loginPage.login(username, password);

        if ("valid".equalsIgnoreCase(expectedStatus)) {
            HomePage homePage = new HomePage(getDriver());
            Assert.assertTrue(homePage.isWelcomeMessageDisplayed(),
                    "Expected successful login for user: " + username);

            SoftAssertHelper.assertTrue(
                    homePage.getWelcomeMessage().toLowerCase().contains("logged"),
                    "Welcome message should indicate a successful login");
            SoftAssertHelper.assertAll();
        } else {
            SoftAssertHelper.assertTrue(loginPage.isErrorMessageDisplayed(),
                    "Error message should be displayed for invalid credentials");
            SoftAssertHelper.assertFalse(loginPage.getErrorMessage().isEmpty(),
                    "Error message should not be blank");
            SoftAssertHelper.assertAll();
        }
    }
}
