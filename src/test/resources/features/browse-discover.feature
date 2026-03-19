Feature: Browse and discover clubs and events

  @participation
  Scenario: Participation dashboard shows charts when data exists
    Given I am logged in as an admin
    And participation data exists
    When I open the participation dashboard
    Then I see participation charts

  @participation
  Scenario: Participation dashboard shows no data message
    Given I am logged in as an admin
    And no participation data exists
    When I open the participation dashboard
    Then I see a participation no data message

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
    When I filter events by category
    And I sort events by date
    Then events are ordered chronologically
