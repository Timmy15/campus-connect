package com.tus.campusconnect.e2e;

import com.tus.campusconnect.e2e.utils.SharedWebDriver;
import com.tus.campusconnect.e2e.utils.UIHelper;
import com.tus.campusconnect.testsupport.TestUsers;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthSteps {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private UIHelper ui;
    private String baseUrl;
    private int lastStatus;
    private final TestUsers users = TestUsers.getInstance();

    @Before
    public void setUp() {
        driver = SharedWebDriver.getDriver();
        ui = UIHelper.getInstance(driver, Duration.ofSeconds(12));
        baseUrl = "http://localhost:" + port;
    }

    @Given("I am on the login page")
    public void iAmOnTheLoginPage() {
        ui.open(baseUrl + "/");
        ui.waitForVisible(By.id("loginForm"));
    }

    @When("I login with email {string} and password {string}")
    public void iLoginWithEmailAndPassword(String email, String password) {
        ui.type(By.id("email"), email);
        ui.type(By.id("password"), password);
        ui.click(By.cssSelector("#loginForm button[type='submit']"));
    }

    @When("I login as {string}")
    public void iLoginAs(String role) {
        String value = role == null ? "" : role.trim().toLowerCase();
        if ("admin".equals(value)) {
            iLoginWithEmailAndPassword(users.getAdminEmail(), users.getAdminPassword());
            return;
        }
        if ("student".equals(value)) {
            iLoginWithEmailAndPassword(users.getStudentEmail(), users.getStudentPassword());
            return;
        }
        throw new IllegalArgumentException("Unknown role: " + role);
    }

    @When("I login with invalid credentials")
    public void iLoginWithInvalidCredentials() {
        iLoginWithEmailAndPassword(users.getInvalidEmail(), users.getWrongPassword());
    }

    @Then("I see a login success message")
    public void iSeeALoginSuccessMessage() {
        ui.waitForText(By.id("loginStatus"), "Login successful");
    }

    @Then("I am redirected to the dashboard page")
    public void iAmRedirectedToTheDashboardPage() {
        ui.waitForUrlContains("/");
        ui.waitForAnyText(By.tagName("body"), "Admin Dashboard", "Student Dashboard");
    }

    @Then("I see a wrong email password error")
    public void iSeeWrongEmailPasswordError() {
        ui.waitForText(By.id("loginStatus"), "Wrong email/password combo.");
    }

    @Then("I remain on the login page")
    public void iRemainOnTheLoginPage() {
        ui.waitForVisible(By.id("loginForm"));
    }

    @When("I attempt to access the admin endpoint")
    public void iAttemptToAccessTheAdminEndpoint() {
        Object status = ui.executeAsyncScript(
                "const callback = arguments[arguments.length - 1];" +
                        "const token = window.localStorage.getItem('cc.token');" +
                        "fetch('" + baseUrl + "/api/admin/ping', {" +
                        "  headers: { Authorization: 'Bearer ' + token }" +
                        "}).then(r => callback(r.status)).catch(() => callback(0));"
        );
        lastStatus = Integer.parseInt(status.toString());
    }

    @Then("I receive a forbidden response")
    public void iReceiveAForbiddenResponse() {
        assertThat(lastStatus).isEqualTo(403);
    }

    @Then("the admin request succeeds")
    public void theAdminRequestSucceeds() {
        assertThat(lastStatus).isEqualTo(200);
    }
}
