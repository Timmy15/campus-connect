Feature: Event management

  Scenario: Create event succeeds
    Given I am logged in as an admin
    And I am on the manage clubs page
    When I create a new club for events
    And I create a new event with valid details
    Then I see the event linked to the club
    And I see the event in the browse events page

  Scenario: Create event fails with invalid capacity
    Given I am logged in as an admin
    And I am on the manage clubs page
    When I create a new club for events
    And I attempt to create an event with invalid capacity
    Then I see an invalid capacity error for events

  Scenario: Update event succeeds
    Given I am logged in as an admin
    And I am on the manage clubs page
    When I create a new club for events
    And I create a new event with valid details
    And I update the event with valid details
    Then I see the updated event linked to the club
    And I see the updated event in the browse events page

  Scenario: Update event fails with past date
    Given I am logged in as an admin
    And I am on the manage clubs page
    When I create a new club for events
    And I create a new event with valid details
    And I attempt to update the event with a past date
    Then I see an invalid date error for events
