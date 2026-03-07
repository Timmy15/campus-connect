Feature: Authentication registration

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'

  Scenario: Student registration and username availability
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def username = 'student' + uuid
    * def email = username + '@student.tus.com'
    * def password = 'abcde'

    Given path '/api/auth/username-available'
    And param username = username
    When method get
    Then status 200
    And match response.available == true

    Given path '/api/auth/register'
    And request { email: '#(email)', username: '#(username)', password: '#(password)' }
    When method post
    Then status 201
    And match response.role == 'STUDENT'

    Given path '/api/auth/username-available'
    And param username = username
    When method get
    Then status 200
    And match response.available == false

  Scenario: Admin registration assigns admin role
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def username = 'admin' + uuid
    * def email = username + '@admin.tus.com'
    * def password = 'abcde'

    Given path '/api/auth/register'
    And request { email: '#(email)', username: '#(username)', password: '#(password)' }
    When method post
    Then status 201
    And match response.role == 'ADMIN'

  Scenario: Registration rejects invalid email domain
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def username = 'user' + uuid

    Given path '/api/auth/register'
    And request { email: 'user@gmail.com', username: '#(username)', password: 'abcde' }
    When method post
    Then status 400
    And match response.message == 'Email must end with @student.tus.com or @admin.tus.com.'

  Scenario: Registration rejects short password
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def username = 'user' + uuid
    * def email = username + '@student.tus.com'

    Given path '/api/auth/register'
    And request { email: '#(email)', username: '#(username)', password: 'abcd' }
    When method post
    Then status 400
    And match response.message == 'Password must be at least 5 characters.'

  Scenario: Registration rejects duplicate email and username
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def username = 'dup' + uuid
    * def email = username + '@student.tus.com'
    * def otherUsername = 'other' + uuid
    * def otherEmail = 'other' + uuid + '@student.tus.com'

    Given path '/api/auth/register'
    And request { email: '#(email)', username: '#(username)', password: 'abcde' }
    When method post
    Then status 201

    Given path '/api/auth/register'
    And request { email: '#(email)', username: '#(otherUsername)', password: 'abcde' }
    When method post
    Then status 409
    And match response.message == 'Email already registered.'

    Given path '/api/auth/register'
    And request { email: '#(otherEmail)', username: '#(username)', password: 'abcde' }
    When method post
    Then status 409
    And match response.message == 'Username already taken.'

  Scenario: Username availability requires a value
    Given path '/api/auth/username-available'
    When method get
    Then status 400
    And match response.message == 'Username is required.'
