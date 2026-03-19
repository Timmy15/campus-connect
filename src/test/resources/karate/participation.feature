Feature: Participation reporting

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'
    * def adminLogin = callonce read('classpath:helpers/login.feature') { email: '#(users.admin.email)', password: '#(users.admin.password)' }
    * def adminAuthHeader = 'Bearer ' + adminLogin.token
    * def studentLogin = callonce read('classpath:helpers/login.feature') { email: '#(users.student.email)', password: '#(users.student.password)' }
    * def studentAuthHeader = 'Bearer ' + studentLogin.token

  Scenario: Participation statistics available
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def clubName = 'StatsClub-' + uuid

    Given path '/api/admin/clubs'
    And header Authorization = adminAuthHeader
    And request { name: '#(clubName)', description: 'Stats club', category: 'Community' }
    When method post
    Then status 201
    * def clubId = response.club.id

    * def startTime = java.time.LocalDateTime.now().plusDays(1).withSecond(0).withNano(0).toString()
    * def eventTitle = 'StatsEvent-' + uuid
    Given path '/api/admin/clubs', clubId, 'events'
    And header Authorization = adminAuthHeader
    And request { title: '#(eventTitle)', description: 'Stats event', location: 'Hall', startTime: '#(startTime)', capacity: 5 }
    When method post
    Then status 201
    * def eventId = response.event.id

    Given path '/api/student/events', eventId, 'register'
    And header Authorization = studentAuthHeader
    When method post
    Then status 201

    Given path '/api/admin/participation'
    And header Authorization = adminAuthHeader
    When method get
    Then status 200
    * def eventStats = response.registrationsPerEvent.find(x => x.eventTitle == eventTitle)
    * match eventStats != null
    * match eventStats.registrationCount == 1
    * def clubStats = response.topClubs.find(x => x.clubName == clubName)
    * match clubStats != null
    * match clubStats.registrationCount == 1
