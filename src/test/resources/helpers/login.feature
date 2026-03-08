Feature: Helper login

  Scenario:
    Given url baseUrl
    And path '/api/auth/login'
    And request { email: '#(email)', password: '#(password)' }
    When method post
    Then status 200
    * def token = response.token
