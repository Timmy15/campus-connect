Feature: Authentication authorization

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'

  Scenario: User profile requires authentication
    Given path '/api/user/me'
    When method get
    Then status 401

  Scenario: Student token is forbidden from admin endpoint
    Given path '/api/auth/login'
    And request { email: 'student@student.tus.com', password: 'Student123' }
    When method post
    Then status 200
    * def token = response.token
    * match token != null

    Given path '/api/admin/ping'
    And header Authorization = 'Bearer ' + token
    When method get
    Then status 403

  Scenario: Admin token can access admin endpoint
    Given path '/api/auth/login'
    And request { email: 'admin@admin.tus.com', password: 'Admin123' }
    When method post
    Then status 200
    * def token = response.token

    Given path '/api/admin/ping'
    And header Authorization = 'Bearer ' + token
    When method get
    Then status 200
