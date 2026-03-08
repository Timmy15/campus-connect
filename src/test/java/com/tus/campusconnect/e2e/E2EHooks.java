package com.tus.campusconnect.e2e;

import com.tus.campusconnect.e2e.utils.SharedWebDriver;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;

public class E2EHooks {

    @Before
    public void beforeScenario() {
        SharedWebDriver.getDriver();
        SharedWebDriver.reset();
    }

    @After
    public void afterScenario() {
        SharedWebDriver.reset();
    }

    @AfterAll
    public static void afterAll() {
        SharedWebDriver.terminateDriver();
    }
}
