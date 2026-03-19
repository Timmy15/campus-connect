Feature: Event management

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'
    * def adminLogin = callonce read('classpath:helpers/login.feature') { email: '#(users.admin.email)', password: '#(users.admin.password)' }
    * def adminAuthHeader = 'Bearer ' + adminLogin.token

  Scenario: Admin creates an event and it appears in browse list
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def clubName = 'Events-' + uuid

    Given path '/api/admin/clubs'
    And header Authorization = adminAuthHeader
    And request { name: '#(clubName)', description: 'Event club', category: 'Community' }
    When method post
    Then status 201
    * def clubId = response.club.id

    * def startTime = java.time.LocalDateTime.now().plusDays(1).withSecond(0).withNano(0).toString()
    * def endTime = java.time.LocalDateTime.parse(startTime).plusHours(2).toString()
    * def eventTitle = 'Launch-' + uuid

    Given path '/api/admin/clubs', clubId, 'events'
    And header Authorization = adminAuthHeader
    And request { title: '#(eventTitle)', description: 'Launch party', location: 'Hall', startTime: '#(startTime)', endTime: '#(endTime)', capacity: 40 }
    When method post
    Then status 201
    And match response.message == 'Event created successfully.'
    And match response.event.title == eventTitle

    Given path '/api/events'
    And header Authorization = adminAuthHeader
    When method get
    Then status 200
    * def titles = $response[*].title
    And match titles contains eventTitle

  Scenario: Event creation fails with invalid capacity
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def clubName = 'Capacity-' + uuid

    Given path '/api/admin/clubs'
    And header Authorization = adminAuthHeader
    And request { name: '#(clubName)', description: 'Event club', category: 'Community' }
    When method post
    Then status 201
    * def clubId = response.club.id

    * def startTime = java.time.LocalDateTime.now().plusDays(1).withSecond(0).withNano(0).toString()

    Given path '/api/admin/clubs', clubId, 'events'
    And header Authorization = adminAuthHeader
    And request { title: 'Invalid', description: 'Invalid', location: 'Hall', startTime: '#(startTime)', capacity: 0 }
    When method post
    Then status 400
    And match response.message == 'Capacity must be greater than 0.'

  Scenario: Event update succeeds
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def clubName = 'Update-' + uuid

    Given path '/api/admin/clubs'
    And header Authorization = adminAuthHeader
    And request { name: '#(clubName)', description: 'Event club', category: 'Community' }
    When method post
    Then status 201
    * def clubId = response.club.id

    * def startTime = java.time.LocalDateTime.now().plusDays(2).withSecond(0).withNano(0).toString()
    * def eventTitle = 'Workshop-' + uuid

    Given path '/api/admin/clubs', clubId, 'events'
    And header Authorization = adminAuthHeader
    And request { title: '#(eventTitle)', description: 'Workshop', location: 'Lab', startTime: '#(startTime)', capacity: 20 }
    When method post
    Then status 201
    * def eventId = response.event.id

    * def updatedTitle = eventTitle + '-Updated'
    * def newStart = java.time.LocalDateTime.now().plusDays(3).withSecond(0).withNano(0).toString()

    Given path '/api/admin/events', eventId
    And header Authorization = adminAuthHeader
    And request { title: '#(updatedTitle)', description: 'Updated', location: 'Lab', startTime: '#(newStart)', capacity: 30 }
    When method put
    Then status 200
    And match response.message == 'Event updated successfully.'
    And match response.event.title == updatedTitle

  Scenario: Event update fails with past date
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def clubName = 'Past-' + uuid

    Given path '/api/admin/clubs'
    And header Authorization = adminAuthHeader
    And request { name: '#(clubName)', description: 'Event club', category: 'Community' }
    When method post
    Then status 201
    * def clubId = response.club.id

    * def startTime = java.time.LocalDateTime.now().plusDays(1).withSecond(0).withNano(0).toString()

    Given path '/api/admin/clubs', clubId, 'events'
    And header Authorization = adminAuthHeader
    And request { title: 'Review', description: 'Review', location: 'Room', startTime: '#(startTime)', capacity: 15 }
    When method post
    Then status 201
    * def eventId = response.event.id

    * def pastTime = java.time.LocalDateTime.now().minusDays(1).withSecond(0).withNano(0).toString()

    Given path '/api/admin/events', eventId
    And header Authorization = adminAuthHeader
    And request { title: 'Review', description: 'Review', location: 'Room', startTime: '#(pastTime)', capacity: 15 }
    When method put
    Then status 400
    And match response.message == 'Start time must be in the future.'
