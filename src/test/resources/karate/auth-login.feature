Feature: Authentication login

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'

  Scenario: Login success with email
    Given path '/api/auth/login'
    And request { email: '#(users.student.email)', password: '#(users.student.password)' }
    When method post
    Then status 200
    And match response.role == 'STUDENT'
    And match response.token != null

  Scenario: Login success with username
    Given path '/api/auth/login'
    And request { email: '#(users.student.username)', password: '#(users.student.password)' }
    When method post
    Then status 200
    And match response.role == 'STUDENT'
    And match response.token != null

  Scenario: Login failure returns unauthorized
    Given path '/api/auth/login'
    And request { email: '#(users.student.email)', password: '#(users.invalidPassword)' }
    When method post
    Then status 401
