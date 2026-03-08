package com.tus.campusconnect.e2e.utils;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.HashMap;
import java.util.Map;

public final class SharedWebDriver {

    private static WebDriver driver;

    private SharedWebDriver() {
    }

    public static WebDriver getDriver() {
        if (driver == null) {
            initialiseDriver();
        }
        return driver;
    }

    public static void reset() {
        if (driver == null) {
            return;
        }
        try {
            driver.manage().deleteAllCookies();
        } catch (Exception ignored) {
            // Best-effort cleanup; failures shouldn't fail the test suite.
        }
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "try { window.localStorage.clear(); window.sessionStorage.clear(); } catch (e) {}"
            );
        } catch (Exception ignored) {
            // Best-effort cleanup; failures shouldn't fail the test suite.
        }
    }

    public static void terminateDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    private static void initialiseDriver() {
        driver = new ChromeDriver(buildChromeOptions());
        configureTimeouts();
        registerShutdownHook();
    }

    private static void configureTimeouts() {
        driver.manage().window().maximize();
    }

    private static ChromeOptions buildChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(getBrowserArguments());
        options.setExperimentalOption("prefs", buildChromePreferences());
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);
        options.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.IGNORE);
        return options;
    }

    private static String[] getBrowserArguments() {
        return new String[]{
                "--headless=new",
                "--disable-notifications",
                "--remote-allow-origins=*",
                "--disable-gpu",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--window-size=1920,1080"
        };
    }

    private static Map<String, Object> buildChromePreferences() {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        prefs.put("profile.default_content_setting_values.notifications", 2);
        return prefs;
    }

    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(SharedWebDriver::terminateDriver));
    }
}
