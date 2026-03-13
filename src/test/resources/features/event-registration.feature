Feature: Event registration

  Scenario: Event registration success
    Given I am logged in as a student
    And I am on the event registration page for an event
    And the capacity for the event is not reached
    When I click register
    Then my registration is stored on the system
    And I get a registration successful message
    And my registration appears in My Registrations

  Scenario: Event registration already registered failure
    Given I am logged in as a student
    And I am on the event registration page for an event
    And I'm already registered for the event
    When I click register
    Then the request is rejected
    And I get a message "You're already registered for this event page"

  Scenario: Event registration capacity reached failure
    Given I am logged in as a student
    And I am on the event registration page for an event
    And the capacity for the event is reached
    When I click register
    Then I get a message "Capacity for this event is reached"
    And I'm not registered for the event
