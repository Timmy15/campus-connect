Feature: Browse and discover clubs and events

  Scenario: Browse Clubs & Events Success
    Given I am logged in as a student
    And there are clubs and events available for browsing
    When I search for the club by name
    Then only the matching club is displayed
    When I filter events by category
    Then only matching events are displayed

  Scenario: Filter Events Success
    Given I am logged in as a student
    And there are clubs and events available for browsing
    When I sort events by date
    Then events are ordered chronologically
