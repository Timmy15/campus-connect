Feature: Authentication login

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'

  Scenario: Login success with email
    Given path '/api/auth/login'
    And request { email: 'student@student.tus.com', password: 'Student123' }
    When method post
    Then status 200
    And match response.role == 'STUDENT'
    And match response.token != null

  Scenario: Login success with username
    Given path '/api/auth/login'
    And request { email: 'student', password: 'Student123' }
    When method post
    Then status 200
    And match response.role == 'STUDENT'
    And match response.token != null

  Scenario: Login failure returns unauthorized
    Given path '/api/auth/login'
    And request { email: 'student@student.tus.com', password: 'WrongPass' }
    When method post
    Then status 401
