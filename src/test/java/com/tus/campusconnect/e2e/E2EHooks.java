package com.tus.campusconnect.e2e;

import com.tus.campusconnect.e2e.utils.SharedWebDriver;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import org.openqa.selenium.WebDriver;
import org.springframework.boot.test.web.server.LocalServerPort;

public class E2EHooks {

    @LocalServerPort
    private int port;

    @Before
    public void beforeScenario() {
        WebDriver driver = SharedWebDriver.getDriver();
        driver.navigate().to(baseUrl());
        SharedWebDriver.reset();
    }

    @After
    public void afterScenario() {
        WebDriver driver = SharedWebDriver.getDriver();
        driver.get(baseUrl());
        SharedWebDriver.reset();
    }

    @AfterAll
    public static void afterAll() {
        SharedWebDriver.terminateDriver();
    }

    private String baseUrl() {
        return "http://localhost:" + port + "/";
    }
}
