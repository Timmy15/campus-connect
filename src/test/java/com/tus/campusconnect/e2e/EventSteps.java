package com.tus.campusconnect.e2e;

import com.tus.campusconnect.e2e.utils.SharedWebDriver;
import com.tus.campusconnect.e2e.utils.UIHelper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EventSteps {

    private static final DateTimeFormatter DATE_TIME_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private WebDriver driver;
    private UIHelper ui;
    private String clubName;
    private String eventTitle;
    private String updatedEventTitle;

    @Before
    public void setUp() {
        driver = SharedWebDriver.getDriver();
        ui = UIHelper.getInstance(driver, Duration.ofSeconds(12));
    }

    @When("I create a new club for events")
    public void iCreateANewClubForEvents() {
        clubName = "Club-" + UUID.randomUUID().toString().substring(0, 8);
        ui.type(By.id("clubName"), clubName);
        ui.type(By.id("clubCategory"), "Community");
        ui.type(By.id("clubDescription"), "Event test club.");
        clickWithFallback(By.id("clubFormSubmit"));
        waitForEventStatus("Club created");
    }

    @When("I create a new event with valid details")
    public void iCreateANewEventWithValidDetails() {
        eventTitle = "Event-" + UUID.randomUUID().toString().substring(0, 8);
        selectClubOption(clubName);
        ui.type(By.id("eventTitle"), eventTitle);
        ui.type(By.id("eventLocation"), "Main Hall");
        ui.type(By.id("eventCapacity"), "25");
        setDateTime(By.id("eventStartTime"), futureDateTime(1, 2));
        setDateTime(By.id("eventEndTime"), futureDateTime(1, 4));
        ui.type(By.id("eventDescription"), "Event description.");
        clickWithFallback(By.id("eventFormSubmit"));
        waitForEventAction(eventRowLocator(eventTitle), "Event created");
    }

    @Then("I see the event linked to the club")
    public void iSeeTheEventLinkedToTheClub() {
        ui.waitForVisible(eventRowLocator(eventTitle));
    }

    @Then("I see the event in the browse events page")
    public void iSeeTheEventInTheBrowseEventsPage() {
        ui.click(By.id("nav-browse-events"));
        ui.waitForVisible(By.id("eventBrowseGrid"));
        ui.waitForVisible(browseEventTitleLocator(eventTitle));
    }

    @When("I attempt to create an event with invalid capacity")
    public void iAttemptToCreateAnEventWithInvalidCapacity() {
        eventTitle = "Event-" + UUID.randomUUID().toString().substring(0, 8);
        selectClubOption(clubName);
        ui.type(By.id("eventTitle"), eventTitle);
        ui.type(By.id("eventLocation"), "Library");
        ui.type(By.id("eventCapacity"), "0");
        setDateTime(By.id("eventStartTime"), futureDateTime(2, 1));
        clickWithFallback(By.id("eventFormSubmit"));
    }

    @Then("I see an invalid capacity error for events")
    public void iSeeAnInvalidCapacityErrorForEvents() {
        ui.waitForText(By.id("eventFormStatus"), "Capacity must be greater than 0.");
        waitForNotPresent(eventRowLocator(eventTitle));
    }

    @When("I update the event with valid details")
    public void iUpdateTheEventWithValidDetails() {
        updatedEventTitle = eventTitle + "-Updated";
        clickWithFallback(eventEditButtonLocator(eventTitle));
        ui.waitForText(By.id("eventFormSubmit"), "Update Event");
        ui.type(By.id("eventTitle"), updatedEventTitle);
        ui.type(By.id("eventLocation"), "Updated Hall");
        ui.type(By.id("eventCapacity"), "30");
        setDateTime(By.id("eventStartTime"), futureDateTime(3, 1));
        setDateTime(By.id("eventEndTime"), futureDateTime(3, 3));
        clickWithFallback(By.id("eventFormSubmit"));
        waitForEventAction(eventRowLocator(updatedEventTitle), "Event updated");
    }

    @Then("I see the updated event linked to the club")
    public void iSeeTheUpdatedEventLinkedToTheClub() {
        ui.waitForVisible(eventRowLocator(updatedEventTitle));
    }

    @Then("I see the updated event in the browse events page")
    public void iSeeTheUpdatedEventInTheBrowseEventsPage() {
        ui.click(By.id("nav-browse-events"));
        ui.waitForVisible(By.id("eventBrowseGrid"));
        ui.waitForVisible(browseEventTitleLocator(updatedEventTitle));
    }

    @When("I attempt to update the event with a past date")
    public void iAttemptToUpdateTheEventWithAPastDate() {
        clickWithFallback(eventEditButtonLocator(eventTitle));
        ui.waitForText(By.id("eventFormSubmit"), "Update Event");
        setDateTime(By.id("eventStartTime"), pastDateTime(1));
        clickWithFallback(By.id("eventFormSubmit"));
    }

    @Then("I see an invalid date error for events")
    public void iSeeAnInvalidDateErrorForEvents() {
        ui.waitForText(By.id("eventFormStatus"), "Start time must be in the future.");
    }

    private void selectClubOption(String name) {
        ui.waitForVisible(By.id("eventClubSelect"));
        By optionLocator = By.xpath("//select[@id='eventClubSelect']/option[normalize-space()='" + name + "']");
        new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(ExpectedConditions.presenceOfElementLocated(optionLocator));
        Select select = new Select(driver.findElement(By.id("eventClubSelect")));
        select.selectByVisibleText(name);
    }

    private String futureDateTime(int daysFromNow, int hoursFromNow) {
        LocalDateTime time = LocalDateTime.now()
                .plusDays(daysFromNow)
                .plusHours(hoursFromNow)
                .withSecond(0)
                .withNano(0);
        return time.format(DATE_TIME_INPUT);
    }

    private String pastDateTime(int daysAgo) {
        LocalDateTime time = LocalDateTime.now()
                .minusDays(daysAgo)
                .withSecond(0)
                .withNano(0);
        return time.format(DATE_TIME_INPUT);
    }

    private By eventRowLocator(String title) {
        return By.xpath("//div[contains(@class,'fw-semibold') and normalize-space()='" + title + "']");
    }

    private By eventEditButtonLocator(String title) {
        return By.xpath("//tr[.//div[contains(@class,'fw-semibold') and normalize-space()='" + title + "']]//button[@data-action='edit-event']");
    }

    private By browseEventTitleLocator(String title) {
        return By.xpath("//h6[normalize-space()='" + title + "']");
    }

    private void clickWithFallback(By locator) {
        try {
            ui.click(locator);
            return;
        } catch (RuntimeException ignored) {
            // Fall back to JavaScript click when Selenium reports an intercepted click.
        }

        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.presenceOfElementLocated(locator));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', inline: 'center'});",
                element
        );
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    private void setDateTime(By locator, String value) {
        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.presenceOfElementLocated(locator));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];" +
                        "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                        "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                element,
                value
        );
    }

    private void waitForNotPresent(By locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));
        boolean gone = wait.until(d -> d.findElements(locator).isEmpty());
        assertThat(gone).isTrue();
    }

    private void waitForEventAction(By successLocator, String expectedStatus) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(d -> {
            if (!d.findElements(successLocator).isEmpty()) {
                return true;
            }
            String status = d.findElement(By.id("eventFormStatus")).getText();
            return status != null && !status.trim().isEmpty();
        });

        if (!driver.findElements(successLocator).isEmpty()) {
            return;
        }

        String status = driver.findElement(By.id("eventFormStatus")).getText();
        if (status == null) {
            status = "";
        }
        if (!status.contains(expectedStatus)) {
            throw new AssertionError("Event action failed: " + status);
        }

        new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(ExpectedConditions.visibilityOfElementLocated(successLocator));
    }

    private void waitForEventStatus(String expectedStatus) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(12));
        wait.until(d -> {
            String status = d.findElement(By.id("clubFormStatus")).getText();
            return status != null && status.contains(expectedStatus);
        });
    }
}
