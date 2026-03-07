Feature: Club Management

  Scenario: Club creation succeeds
    Given I am logged in as an admin
    And I am on the manage clubs page
    When I create a new club
    Then I see the club in the manage list
    And I see the club in the browse clubs page

  Scenario: Club update succeeds
    Given I am logged in as an admin
    And I am on the manage clubs page
    When I create a new club
    And I update the club name
    Then I see the updated club in the manage list
    And I see the updated club in the browse clubs page

  Scenario: Club deactivation hides the club
    Given I am logged in as an admin
    And I am on the manage clubs page
    When I create a new club
    And I deactivate the club
    Then the club is hidden from the browse clubs page

  Scenario: Duplicate club names are rejected
    Given I am logged in as an admin
    And I am on the manage clubs page
    When I create a new club
    And I attempt to create another club with the same name
    Then I see a club already exists error
