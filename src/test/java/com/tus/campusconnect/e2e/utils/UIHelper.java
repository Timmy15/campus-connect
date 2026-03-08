package com.tus.campusconnect.e2e.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public final class UIHelper {
    private static UIHelper instance;
    private WebDriver driver;
    private WebDriverWait wait;

    private UIHelper(WebDriver driver, Duration timeout) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, timeout);
    }

    public static synchronized UIHelper getInstance(WebDriver driver, Duration timeout) {
        if (instance == null) {
            instance = new UIHelper(driver, timeout);
        } else {
            instance.driver = driver;
            instance.wait = new WebDriverWait(driver, timeout);
        }
        return instance;
    }

    public void open(String url) {
        driver.get(url);
    }

    public void type(By locator, String value) {
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        element.clear();
        element.sendKeys(value);
    }

    public void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    public void waitForVisible(By locator) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public void waitForText(By locator, String text) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    public void waitForAnyText(By locator, String first, String second) {
        wait.until(ExpectedConditions.or(
                ExpectedConditions.textToBePresentInElementLocated(locator, first),
                ExpectedConditions.textToBePresentInElementLocated(locator, second)
        ));
    }

    public void waitForUrlContains(String fragment) {
        wait.until(ExpectedConditions.urlContains(fragment));
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }

    public Object executeAsyncScript(String script) {
        return ((JavascriptExecutor) driver).executeAsyncScript(script);
    }

    public Object executeAsyncScript(String script, Object arg) {
        return ((JavascriptExecutor) driver).executeAsyncScript(script, arg);
    }
}
