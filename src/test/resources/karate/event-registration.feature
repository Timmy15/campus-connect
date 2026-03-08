Feature: Event registration

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'
    * def adminLogin = callonce read('classpath:helpers/login.feature') { email: '#(users.admin.email)', password: '#(users.admin.password)' }
    * def adminAuthHeader = 'Bearer ' + adminLogin.token
    * def studentLogin = callonce read('classpath:helpers/login.feature') { email: '#(users.student.email)', password: '#(users.student.password)' }
    * def studentAuthHeader = 'Bearer ' + studentLogin.token

  Scenario: Event registration success
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def clubName = 'RegClub-' + uuid

    Given path '/api/admin/clubs'
    And header Authorization = adminAuthHeader
    And request { name: '#(clubName)', description: 'Registration club', category: 'Community' }
    When method post
    Then status 201
    * def clubId = response.club.id

    * def startTime = java.time.LocalDateTime.now().plusDays(1).withSecond(0).withNano(0).toString()
    * def eventTitle = 'RegEvent-' + uuid
    Given path '/api/admin/clubs', clubId, 'events'
    And header Authorization = adminAuthHeader
    And request { title: '#(eventTitle)', description: 'Registration event', location: 'Hall', startTime: '#(startTime)', capacity: 2 }
    When method post
    Then status 201
    * def eventId = response.event.id

    Given path '/api/student/events', eventId, 'register'
    And header Authorization = studentAuthHeader
    When method post
    Then status 201
    And match response.message == 'Registration successful.'
    And match response.event.id == eventId

  Scenario: Event registration already registered failure
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def clubName = 'RegDup-' + uuid

    Given path '/api/admin/clubs'
    And header Authorization = adminAuthHeader
    And request { name: '#(clubName)', description: 'Registration club', category: 'Community' }
    When method post
    Then status 201
    * def clubId = response.club.id

    * def startTime = java.time.LocalDateTime.now().plusDays(1).withSecond(0).withNano(0).toString()
    * def eventTitle = 'RegDupEvent-' + uuid
    Given path '/api/admin/clubs', clubId, 'events'
    And header Authorization = adminAuthHeader
    And request { title: '#(eventTitle)', description: 'Registration event', location: 'Hall', startTime: '#(startTime)', capacity: 2 }
    When method post
    Then status 201
    * def eventId = response.event.id

    Given path '/api/student/events', eventId, 'register'
    And header Authorization = studentAuthHeader
    When method post
    Then status 201

    Given path '/api/student/events', eventId, 'register'
    And header Authorization = studentAuthHeader
    When method post
    Then status 409
    And match response.message == "You're already registered for this event page"

  Scenario: Event registration capacity reached failure
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def clubName = 'RegCap-' + uuid

    Given path '/api/admin/clubs'
    And header Authorization = adminAuthHeader
    And request { name: '#(clubName)', description: 'Registration club', category: 'Community' }
    When method post
    Then status 201
    * def clubId = response.club.id

    * def startTime = java.time.LocalDateTime.now().plusDays(1).withSecond(0).withNano(0).toString()
    * def eventTitle = 'RegCapEvent-' + uuid
    Given path '/api/admin/clubs', clubId, 'events'
    And header Authorization = adminAuthHeader
    And request { title: '#(eventTitle)', description: 'Registration event', location: 'Hall', startTime: '#(startTime)', capacity: 1 }
    When method post
    Then status 201
    * def eventId = response.event.id

    Given path '/api/student/events', eventId, 'register'
    And header Authorization = adminAuthHeader
    When method post
    Then status 201

    Given path '/api/student/events', eventId, 'register'
    And header Authorization = studentAuthHeader
    When method post
    Then status 409
    And match response.message == 'Capacity for this event is reached'
