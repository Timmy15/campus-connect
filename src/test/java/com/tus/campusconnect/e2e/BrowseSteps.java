package com.tus.campusconnect.e2e;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tus.campusconnect.e2e.utils.SharedWebDriver;
import com.tus.campusconnect.e2e.utils.UIHelper;
import com.tus.campusconnect.testsupport.TestUsers;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class BrowseSteps {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @LocalServerPort
    private int port;

    private final TestUsers users = TestUsers.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();

    private WebDriver driver;
    private UIHelper ui;
    private String baseUrl;

    private String clubNameMatch;
    private String clubNameOther;
    private String clubCategoryMatch;
    private String eventTitleEarly;
    private String eventTitleLate;
    private String eventTitleOther;

    @Before
    public void setUp() {
        driver = SharedWebDriver.getDriver();
        ui = UIHelper.getInstance(driver, Duration.ofSeconds(12));
        baseUrl = "http://localhost:" + port;
    }

    @Given("I am logged in as a student")
    public void iAmLoggedInAsAStudent() {
        LoginResult login = loginViaApi(users.getStudentEmail(), users.getStudentPassword());
        ui.open(baseUrl + "/login.html");
        setAuthStorage(login);
        driver.navigate().to(baseUrl + "/");
        ui.waitForVisible(By.id("nav-browse-clubs"));
    }

    @Given("there are clubs and events available for browsing")
    public void thereAreClubsAndEventsAvailableForBrowsing() {
        LoginResult admin = loginViaApi(users.getAdminEmail(), users.getAdminPassword());

        clubCategoryMatch = "Category-" + UUID.randomUUID().toString().substring(0, 6);
        clubNameMatch = "BrowseClub-" + UUID.randomUUID().toString().substring(0, 6);
        clubNameOther = "OtherClub-" + UUID.randomUUID().toString().substring(0, 6);

        long matchClubId = createClub(admin.token, clubNameMatch, clubCategoryMatch);
        long otherClubId = createClub(admin.token, clubNameOther, "Category-" + UUID.randomUUID().toString().substring(0, 6));

        eventTitleEarly = "EventEarly-" + UUID.randomUUID().toString().substring(0, 6);
        eventTitleLate = "EventLate-" + UUID.randomUUID().toString().substring(0, 6);
        eventTitleOther = "EventOther-" + UUID.randomUUID().toString().substring(0, 6);

        LocalDateTime now = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
        createEvent(admin.token, matchClubId, eventTitleEarly, now.plusHours(1));
        createEvent(admin.token, matchClubId, eventTitleLate, now.plusHours(2));
        createEvent(admin.token, otherClubId, eventTitleOther, now.plusHours(3));
    }

    @When("I search for the club by name")
    public void iSearchForTheClubByName() {
        ui.click(By.id("nav-browse-clubs"));
        waitForBrowseCount("clubBrowseCount");

        ui.type(By.id("clubSearchInput"), clubNameMatch);
    }

    @Then("only the matching club is displayed")
    public void onlyTheMatchingClubIsDisplayed() {
        ui.waitForVisible(clubTitleLocator(clubNameMatch));
        waitForNotPresent(clubTitleLocator(clubNameOther));
    }

    @When("I filter events by category")
    public void iFilterEventsByCategory() {
        ui.click(By.id("nav-browse-events"));
        waitForBrowseCount("eventBrowseCount");

        Select select = new Select(driver.findElement(By.id("eventCategoryFilter")));
        select.selectByVisibleText(clubCategoryMatch);
    }

    @Then("only matching events are displayed")
    public void onlyMatchingEventsAreDisplayed() {
        ui.waitForVisible(eventTitleLocator(eventTitleLate));
        waitForNotPresent(eventTitleLocator(eventTitleOther));
    }

    @When("I sort events by date")
    public void iSortEventsByDate() {
        ui.waitForVisible(By.id("eventSortSelect"));

        Select select = new Select(driver.findElement(By.id("eventSortSelect")));
        select.selectByValue("date-asc");
    }

    @Then("events are ordered chronologically")
    public void eventsAreOrderedChronologically() {
        new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(d -> d.findElements(By.cssSelector("#eventBrowseGrid .card h6")).size() >= 2);

        List<WebElement> titles = driver.findElements(By.cssSelector("#eventBrowseGrid .card h6"));
        String first = titles.get(0).getText().trim();
        String second = titles.get(1).getText().trim();

        assertThat(List.of(first, second)).contains(eventTitleEarly, eventTitleLate);

        assertThat(first).isEqualTo(eventTitleEarly);
        assertThat(second).isEqualTo(eventTitleLate);
    }

    private long createClub(String token, String name, String category) {
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "name", name,
                    "category", category,
                    "description", "Browse discovery test club."
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/admin/clubs"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                throw new IllegalStateException("Failed to create club: " + response.statusCode());
            }

            Map<String, Object> payload = mapper.readValue(response.body(), new TypeReference<>() {});
            Map<String, Object> club = (Map<String, Object>) payload.get("club");
            return ((Number) club.get("id")).longValue();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create club via API.", ex);
        }
    }

    private void createEvent(String token, long clubId, String title, LocalDateTime startTime) {
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "title", title,
                    "description", "Browse discovery event.",
                    "location", "Main Hall",
                    "capacity", 25,
                    "startTime", startTime.format(DATE_TIME_FORMAT),
                    "endTime", startTime.plusHours(1).format(DATE_TIME_FORMAT)
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/admin/clubs/" + clubId + "/events"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                throw new IllegalStateException("Failed to create event: " + response.statusCode());
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create event via API.", ex);
        }
    }

    private LoginResult loginViaApi(String email, String password) {
        try {
            String body = mapper.writeValueAsString(Map.of("email", email, "password", password));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("Login failed with status " + response.statusCode());
            }

            Map<String, Object> payload = mapper.readValue(response.body(), new TypeReference<>() {});
            String token = payload.get("token") != null ? payload.get("token").toString() : null;
            String role = payload.get("role") != null ? payload.get("role").toString() : null;

            if (token == null || role == null) {
                throw new IllegalStateException("Login response missing token or role.");
            }

            return new LoginResult(token, role);
        } catch (Exception ex) {
            throw new RuntimeException("API login failed.", ex);
        }
    }

    private void setAuthStorage(LoginResult login) {
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "localStorage.setItem('cc.token', arguments[0]);" +
                        "localStorage.setItem('cc.role', arguments[1]);",
                login.token,
                login.role
        );
    }

    private void waitForBrowseCount(String countId) {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.not(
                        ExpectedConditions.textToBe(By.id(countId), "Loading...")
                ));
    }

    private void waitForNotPresent(By locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));
        boolean gone = wait.until(d -> d.findElements(locator).isEmpty());
        assertThat(gone).isTrue();
    }

    private By clubTitleLocator(String name) {
        return By.xpath("//h6[normalize-space()='" + name + "']");
    }

    private By eventTitleLocator(String title) {
        return By.xpath("//h6[normalize-space()='" + title + "']");
    }

    private static final class LoginResult {
        private final String token;
        private final String role;

        private LoginResult(String token, String role) {
            this.token = token;
            this.role = role;
        }
    }
}
