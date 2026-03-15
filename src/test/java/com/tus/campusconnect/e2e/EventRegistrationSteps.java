package com.tus.campusconnect.e2e;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tus.campusconnect.e2e.utils.SharedWebDriver;
import com.tus.campusconnect.e2e.utils.UIHelper;
import com.tus.campusconnect.model.RegistrationStatus;
import com.tus.campusconnect.model.User;
import com.tus.campusconnect.repository.EventRegistrationRepository;
import com.tus.campusconnect.repository.EventRepository;
import com.tus.campusconnect.repository.UserRepository;
import com.tus.campusconnect.testsupport.TestUsers;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EventRegistrationSteps {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final Object DATA_LOCK = new Object();
    private static boolean seedReady = false;
    private static EventContext openEvent;
    private static EventContext registeredEvent;
    private static EventContext fullEvent;
    private static EventContext cancelEvent;
    private static String cachedAdminToken;
    private static String cachedStudentToken;
    private static long cachedStudentId;

    @LocalServerPort
    private int port;

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private final TestUsers users = TestUsers.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();

    private WebDriver driver;
    private UIHelper ui;
    private String baseUrl;
    private String scenarioName;

    private long eventId;
    private String eventTitle;
    private int eventCapacity = 1;

    private String adminToken;
    private String studentToken;
    private long studentId;

    @Before
    public void setUp(Scenario scenario) {
        driver = SharedWebDriver.getDriver();
        ui = UIHelper.getInstance(driver, Duration.ofSeconds(12));
        baseUrl = "http://localhost:" + port;
        scenarioName = scenario != null ? scenario.getName() : "";
        ensureSeedData();
        adminToken = cachedAdminToken;
        studentToken = cachedStudentToken;
        studentId = cachedStudentId;
    }

    @Given("I am on the event registration page for an event")
    public void iAmOnTheEventRegistrationPageForAnEvent() {
        EventContext context = selectEventForScenario();
        eventTitle = context.title;
        eventId = context.id;
        eventCapacity = context.capacity;
        if (studentToken != null) {
            setAuthStorage(new LoginResult(studentToken, "ROLE_STUDENT"));
        }
        ui.click(By.id("nav-browse-events"));
        waitForBrowseCount("eventBrowseCount");
        ui.waitForVisible(eventTitleLocator(eventTitle));
    }

    @Given("the capacity for the event is not reached")
    public void theCapacityForTheEventIsNotReached() {
        long count = eventRegistrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED);
        assertThat(count).isLessThan(eventCapacity);
    }

    @Given("I'm already registered for the event")
    public void imAlreadyRegisteredForTheEvent() {
        boolean alreadyRegistered = eventRegistrationRepository
                .existsByEventIdAndUserIdAndStatus(eventId, studentId, RegistrationStatus.REGISTERED);
        if (!alreadyRegistered) {
            int status = registerEvent(studentToken, eventId);
            assertThat(status).isEqualTo(201);
            refreshEventBrowse();
        }
        assertThat(eventRegistrationRepository.existsByEventIdAndUserIdAndStatus(eventId, studentId, RegistrationStatus.REGISTERED))
                .isTrue();
    }

    @Given("the capacity for the event is reached")
    public void theCapacityForTheEventIsReached() {
        long count = eventRegistrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED);
        if (count < eventCapacity) {
            int status = registerEvent(adminToken, eventId);
            assertThat(status).isEqualTo(201);
            refreshEventBrowse();
        }
        long updatedCount = eventRegistrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED);
        assertThat(updatedCount).isGreaterThanOrEqualTo(eventCapacity);
    }

    @When("I click register")
    public void iClickRegister() {
        ui.click(registerButtonLocator(eventTitle));
    }

    @Then("my registration is stored on the system")
    public void myRegistrationIsStoredOnTheSystem() {
        boolean registered = new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(d -> isStudentRegistered());
        assertThat(registered).isTrue();
    }

    @Then("I get a registration successful message")
    public void iGetARegistrationSuccessfulMessage() {
        ui.waitForText(By.id("eventBrowseStatus"), "Registration successful.");
    }

    @Then("my registration appears in My Registrations")
    public void myRegistrationAppearsInMyRegistrations() {
        ui.click(By.id("nav-my-registrations"));
        waitForBrowseCount("registrationCount");
        ui.waitForVisible(By.xpath("//td[normalize-space()='" + eventTitle + "']"));
    }

    @Then("the request is rejected")
    public void theRequestIsRejected() {
        WebElement button = driver.findElement(registerButtonLocator(eventTitle));
        assertThat(button.isEnabled()).isFalse();
        WebElement note = new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(ExpectedConditions.visibilityOfElementLocated(registrationNoteLocator(eventTitle)));
        String text = note.getText() == null ? "" : note.getText().trim();
        assertThat(text).isNotEmpty();
    }

    @Then("I get a message {string}")
    public void iGetAMessage(String message) {
        boolean found = new WebDriverWait(driver, Duration.ofSeconds(12))
                .until(d -> {
                    try {
                        String statusText = readStatusText();
                        if (statusText.contains(message)) {
                            return true;
                        }
                        for (WebElement note : d.findElements(registrationNoteLocator(eventTitle))) {
                            String noteText = note.getText();
                            if (noteText != null && noteText.contains(message)) {
                                return true;
                            }
                        }
                        return false;
                    } catch (StaleElementReferenceException ex) {
                        return false;
                    }
                });
        assertThat(found).isTrue();
    }

    @Then("I'm not registered for the event")
    public void imNotRegisteredForTheEvent() {
        boolean registered = eventRegistrationRepository.existsByEventIdAndUserIdAndStatus(
                eventId,
                studentId,
                RegistrationStatus.REGISTERED
        );
        assertThat(registered).isFalse();
    }

    @When("I cancel my registration")
    public void iCancelMyRegistration() {
        ui.click(By.id("nav-my-registrations"));
        waitForBrowseCount("registrationCount");
        ui.waitForVisible(cancelButtonLocator(eventTitle));
        ui.click(cancelButtonLocator(eventTitle));
        ui.acceptConfirm();
    }

    @Then("the registration is removed from the system")
    public void theRegistrationIsRemovedFromTheSystem() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));
        boolean removed = wait.until(d -> !isStudentRegistered());
        assertThat(removed).isTrue();
        wait.until(d -> d.findElements(registrationRowLocator(eventTitle)).isEmpty());
    }

    @Then("I get a cancellation successful message")
    public void iGetACancellationSuccessfulMessage() {
        ui.waitForText(By.id("registrationStatus"), "Registration cancelled successfully.");
    }

    private By eventTitleLocator(String title) {
        return By.xpath("//h6[normalize-space()='" + title + "']");
    }

    private By registerButtonLocator(String title) {
        return By.xpath("//div[contains(@class,'card')][.//h6[normalize-space()='" + title + "']]//button[@data-action='register']");
    }

    private By registrationNoteLocator(String title) {
        return By.xpath("//div[contains(@class,'card')][.//h6[normalize-space()='" + title + "']]//div[@data-role='registration-note']");
    }

    private By cancelButtonLocator(String title) {
        return By.xpath("//tr[.//td[contains(@class,'fw-semibold') and normalize-space()='" + title + "']]//button[@data-action='cancel-registration']");
    }

    private By registrationRowLocator(String title) {
        return By.xpath("//tr[.//td[contains(@class,'fw-semibold') and normalize-space()='" + title + "']]");
    }

    private void waitForBrowseCount(String countId) {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.not(
                        ExpectedConditions.textToBe(By.id(countId), "Loading...")
                ));
    }

    private void refreshEventBrowse() {
        ui.click(By.id("nav-browse-events"));
        waitForBrowseCount("eventBrowseCount");
        ui.waitForVisible(eventTitleLocator(eventTitle));
    }

    private String readStatusText() {
        WebElement status = driver.findElements(By.id("eventBrowseStatus"))
                .stream()
                .findFirst()
                .orElse(null);
        if (status == null) {
            return "";
        }
        String text = status.getText();
        return text == null ? "" : text.trim();
    }

    private void setAuthStorage(LoginResult login) {
        ((JavascriptExecutor) driver).executeScript(
                "localStorage.setItem('cc.token', arguments[0]);" +
                        "localStorage.setItem('cc.role', arguments[1]);",
                login.token,
                login.role
        );
    }

    private boolean isStudentRegistered() {
        return eventRegistrationRepository.existsByEventIdAndUserIdAndStatus(
                eventId,
                studentId,
                RegistrationStatus.REGISTERED
        );
    }

    private long lookupUserId(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
        return user.getId();
    }

    private long createClub(String token, String name) {
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "name", name,
                    "category", "Community",
                    "description", "Registration test club."
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

    private long createEvent(String token, long clubId, String title, int capacity) {
        try {
            LocalDateTime start = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
            String body = mapper.writeValueAsString(Map.of(
                    "title", title,
                    "description", "Registration event.",
                    "location", "Main Hall",
                    "capacity", capacity,
                    "startTime", start.format(DATE_TIME_FORMAT),
                    "endTime", start.plusHours(1).format(DATE_TIME_FORMAT)
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

            Map<String, Object> payload = mapper.readValue(response.body(), new TypeReference<>() {});
            Map<String, Object> event = (Map<String, Object>) payload.get("event");
            return ((Number) event.get("id")).longValue();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create event via API.", ex);
        }
    }

    private int registerEvent(String token, long eventId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/student/events/" + eventId + "/register"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to register via API.", ex);
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

    private static final class LoginResult {
        private final String token;
        private final String role;

        private LoginResult(String token, String role) {
            this.token = token;
            this.role = role;
        }
    }

    private static final class EventContext {
        private final long id;
        private final String title;
        private final int capacity;

        private EventContext(long id, String title, int capacity) {
            this.id = id;
            this.title = title;
            this.capacity = capacity;
        }
    }

    private EventContext selectEventForScenario() {
        String name = scenarioName == null ? "" : scenarioName.toLowerCase();
        if (name.contains("cancel")) {
            return cancelEvent;
        }
        if (name.contains("already registered")) {
            return registeredEvent;
        }
        if (name.contains("capacity reached")) {
            return fullEvent;
        }
        return openEvent;
    }

    private void ensureSeedData() {
        if (seedReady && seedDataAvailable()) {
            return;
        }
        synchronized (DATA_LOCK) {
            if (seedReady && seedDataAvailable()) {
                return;
            }
            seedReady = false;

            LoginResult admin = loginViaApi(users.getAdminEmail(), users.getAdminPassword());
            LoginResult student = loginViaApi(users.getStudentEmail(), users.getStudentPassword());
            cachedAdminToken = admin.token;
            cachedStudentToken = student.token;
            cachedStudentId = lookupUserId(users.getStudentEmail());

            String clubName = "RegClub-" + UUID.randomUUID().toString().substring(0, 8);
            long clubId = createClub(cachedAdminToken, clubName);

            openEvent = createEventContext(clubId, "RegEvent-Open");
            registeredEvent = createEventContext(clubId, "RegEvent-Registered");
            fullEvent = createEventContext(clubId, "RegEvent-Full", 1);
            cancelEvent = createEventContext(clubId, "RegEvent-Cancel");

            int registeredStatus = registerEvent(cachedStudentToken, registeredEvent.id);
            if (registeredStatus != 201) {
                throw new IllegalStateException("Failed to seed registered event: " + registeredStatus);
            }

            int fullStatus = registerEvent(cachedAdminToken, fullEvent.id);
            if (fullStatus != 201) {
                throw new IllegalStateException("Failed to seed full event: " + fullStatus);
            }

            int cancelStatus = registerEvent(cachedStudentToken, cancelEvent.id);
            if (cancelStatus != 201) {
                throw new IllegalStateException("Failed to seed cancel event: " + cancelStatus);
            }

            seedReady = true;
        }
    }

    private boolean seedDataAvailable() {
        if (openEvent == null || registeredEvent == null || fullEvent == null || cancelEvent == null) {
            return false;
        }
        return eventRepository.existsById(openEvent.id)
                && eventRepository.existsById(registeredEvent.id)
                && eventRepository.existsById(fullEvent.id)
                && eventRepository.existsById(cancelEvent.id);
    }

    private EventContext createEventContext(long clubId, String baseTitle) {
        return createEventContext(clubId, baseTitle, 2);
    }

    private EventContext createEventContext(long clubId, String baseTitle, int capacity) {
        String title = baseTitle + "-" + UUID.randomUUID().toString().substring(0, 8);
        long id = createEvent(cachedAdminToken, clubId, title, capacity);
        return new EventContext(id, title, capacity);
    }
}
