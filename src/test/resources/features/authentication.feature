Feature: Secure User Authentication

  Scenario: Login success
    Given I am on the login page
    When I login with email "student@student.tus.com" and password "Student123"
    Then I see a login success message
    And I am redirected to the dashboard page

  Scenario: Login failure
    Given I am on the login page
    When I login with email "invalid@student.tus.com" and password "WrongPass"
    Then I see a wrong email password error
    And I remain on the login page

  Scenario: Student cannot access admin endpoint
    Given I am on the login page
    When I login with email "student@student.tus.com" and password "Student123"
    Then I am redirected to the dashboard page
    When I attempt to access the admin endpoint
    Then I receive a forbidden response

  Scenario: Admin can access admin endpoint
    Given I am on the login page
    When I login with email "admin@admin.tus.com" and password "Admin123"
    Then I am redirected to the dashboard page
    When I attempt to access the admin endpoint
    Then the admin request succeeds
