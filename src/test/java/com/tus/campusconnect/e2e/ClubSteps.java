package com.tus.campusconnect.e2e;

import com.tus.campusconnect.e2e.utils.SharedWebDriver;
import com.tus.campusconnect.e2e.utils.UIHelper;
import com.tus.campusconnect.testsupport.TestUsers;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ClubSteps {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private UIHelper ui;
    private String baseUrl;
    private String clubName;
    private String updatedClubName;
    private final TestUsers users = TestUsers.getInstance();

    @Before
    public void setUp() {
        driver = SharedWebDriver.getDriver();
        ui = UIHelper.getInstance(driver, Duration.ofSeconds(12));
        baseUrl = "http://localhost:" + port;
    }

    @Given("I am logged in as an admin")
    public void iAmLoggedInAsAnAdmin() {
        ui.open(baseUrl + "/login.html");
        ui.waitForVisible(By.id("loginForm"));
        ui.type(By.id("email"), users.getAdminEmail());
        ui.type(By.id("password"), users.getAdminPassword());
        ui.click(By.cssSelector("#loginForm button[type='submit']"));
        ui.waitForText(By.id("loginStatus"), "Login successful");
        ui.waitForUrlContains("/");
        ui.waitForVisible(By.id("nav-manage-clubs"));
    }

    @Given("I am on the manage clubs page")
    public void iAmOnTheManageClubsPage() {
        ui.click(By.id("nav-manage-clubs"));
        ui.waitForVisible(By.id("clubForm"));
    }

    @When("I create a new club")
    public void iCreateANewClub() {
        clubName = "Club-" + UUID.randomUUID().toString().substring(0, 8);
        ui.type(By.id("clubName"), clubName);
        ui.type(By.id("clubCategory"), "Tech");
        ui.type(By.id("clubDescription"), "Automation test club.");
        clickWithFallback(By.id("clubFormSubmit"));
        waitForClubAction(clubNameCellLocator(clubName), "Club created");
    }

    @Then("I see the club in the manage list")
    public void iSeeTheClubInTheManageList() {
        ui.waitForVisible(clubNameCellLocator(clubName));
    }

    @When("I update the club name")
    public void iUpdateTheClubName() {
        updatedClubName = clubName + "-Updated";
        ui.waitForVisible(clubNameCellLocator(clubName));
        clickWithFallback(editButtonLocator(clubName));
        ui.waitForText(By.id("clubFormSubmit"), "Update Club");
        ui.type(By.id("clubName"), updatedClubName);
        clickWithFallback(By.id("clubFormSubmit"));
        waitForClubAction(clubNameCellLocator(updatedClubName), "Club updated");
    }

    @Then("I see the updated club in the manage list")
    public void iSeeTheUpdatedClubInTheManageList() {
        ui.waitForVisible(clubNameCellLocator(updatedClubName));
    }

    @Then("I see the updated club in the browse clubs page")
    public void iSeeTheUpdatedClubInTheBrowseClubsPage() {
        ui.click(By.id("nav-browse-clubs"));
        ui.waitForVisible(By.id("clubBrowseGrid"));
        waitForBrowseLoaded();
        ui.waitForVisible(browseCardTitleLocator(updatedClubName));
    }

    @Then("I see the club in the browse clubs page")
    public void iSeeTheClubInTheBrowseClubsPage() {
        ui.click(By.id("nav-browse-clubs"));
        ui.waitForVisible(By.id("clubBrowseGrid"));
        waitForBrowseLoaded();
        ui.waitForVisible(browseCardTitleLocator(clubName));
    }

    @When("I deactivate the club")
    public void iDeactivateTheClub() {
        ui.click(By.id("nav-manage-clubs"));
        ui.waitForVisible(By.id("clubForm"));
        String name = updatedClubName != null ? updatedClubName : clubName;
        ui.waitForVisible(clubNameCellLocator(name));
        clickWithFallback(deactivateButtonLocator(name));
        acceptConfirm();
        waitForClubAction(inactiveBadgeLocator(name), "Club deactivated");
        ui.waitForVisible(inactiveBadgeLocator(name));
    }

    @Then("the club is hidden from the browse clubs page")
    public void theClubIsHiddenFromTheBrowseClubsPage() {
        ui.click(By.id("nav-browse-clubs"));
        ui.waitForVisible(By.id("clubBrowseGrid"));
        waitForBrowseLoaded();
        String name = updatedClubName != null ? updatedClubName : clubName;
        waitForNotPresent(browseCardTitleLocator(name));
    }

    @When("I attempt to create another club with the same name")
    public void iAttemptToCreateAnotherClubWithTheSameName() {
        ui.type(By.id("clubName"), clubName);
        ui.type(By.id("clubCategory"), "Tech");
        ui.type(By.id("clubDescription"), "Duplicate attempt.");
        clickWithFallback(By.id("clubFormSubmit"));
    }

    @Then("I see a club already exists error")
    public void iSeeAClubAlreadyExistsError() {
        ui.waitForText(By.id("clubFormStatus"), "Club already exists");
    }

    private By clubNameCellLocator(String name) {
        return By.xpath("//div[contains(@class,'fw-semibold') and normalize-space()='" + name + "']");
    }

    private By editButtonLocator(String name) {
        return By.xpath("//tr[.//div[contains(@class,'fw-semibold') and normalize-space()='" + name + "']]//button[@data-action='edit']");
    }

    private By deactivateButtonLocator(String name) {
        return By.xpath("//tr[.//div[contains(@class,'fw-semibold') and normalize-space()='" + name + "']]//button[@data-action='deactivate']");
    }

    private By inactiveBadgeLocator(String name) {
        return By.xpath("//tr[.//div[contains(@class,'fw-semibold') and normalize-space()='" + name + "']]//span[normalize-space()='Inactive']");
    }

    private By browseCardTitleLocator(String name) {
        return By.xpath("//h6[normalize-space()='" + name + "']");
    }

    private void acceptConfirm() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();
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

    private void waitForNotPresent(By locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));
        boolean gone = wait.until(d -> d.findElements(locator).isEmpty());
        assertThat(gone).isTrue();
    }

    private void waitForBrowseLoaded() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));
        wait.until(d -> {
            String text = d.findElement(By.id("clubBrowseCount")).getText();
            return text != null && !text.contains("Loading");
        });
    }

    private void waitForClubAction(By successLocator, String expectedStatus) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(d -> {
            if (!d.findElements(successLocator).isEmpty()) {
                return true;
            }
            String status = d.findElement(By.id("clubFormStatus")).getText();
            return status != null && !status.trim().isEmpty();
        });

        if (!driver.findElements(successLocator).isEmpty()) {
            return;
        }

        String status = driver.findElement(By.id("clubFormStatus")).getText();
        if (status == null) {
            status = "";
        }
        if (!status.contains(expectedStatus)) {
            throw new AssertionError("Club action failed: " + status);
        }

        new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(ExpectedConditions.visibilityOfElementLocated(successLocator));
    }
}
