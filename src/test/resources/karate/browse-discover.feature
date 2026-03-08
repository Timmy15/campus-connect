Feature: Browse and discover clubs and events

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'
    * def adminLogin = callonce read('classpath:helpers/login.feature') { email: '#(users.admin.email)', password: '#(users.admin.password)' }
    * def adminAuthHeader = 'Bearer ' + adminLogin.token

  Scenario: Browse clubs and events with category and sorted dates
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def clubNameA = 'BrowseA-' + uuid
    * def clubNameB = 'BrowseB-' + uuid
    * def categoryA = 'Arts'
    * def categoryB = 'Tech'

    Given path '/api/admin/clubs'
    And header Authorization = adminAuthHeader
    And request { name: '#(clubNameA)', description: 'Browse club A', category: '#(categoryA)' }
    When method post
    Then status 201
    * def clubIdA = response.club.id

    Given path '/api/admin/clubs'
    And header Authorization = adminAuthHeader
    And request { name: '#(clubNameB)', description: 'Browse club B', category: '#(categoryB)' }
    When method post
    Then status 201
    * def clubIdB = response.club.id

    Given path '/api/clubs'
    When method get
    Then status 200
    * def clubA = response.find(x => x.id == clubIdA)
    * def clubB = response.find(x => x.id == clubIdB)
    And match clubA.category == categoryA
    And match clubB.category == categoryB

    * def startEarly = java.time.LocalDateTime.now().plusDays(1).withSecond(0).withNano(0).toString()
    * def startLate = java.time.LocalDateTime.parse(startEarly).plusHours(2).toString()
    * def titleEarly = 'EventEarly-' + uuid
    * def titleLate = 'EventLate-' + uuid

    Given path '/api/admin/clubs', clubIdA, 'events'
    And header Authorization = adminAuthHeader
    And request { title: '#(titleEarly)', description: 'Early event', location: 'Hall', startTime: '#(startEarly)', capacity: 10 }
    When method post
    Then status 201
    And match response.event.clubCategory == categoryA

    Given path '/api/admin/clubs', clubIdB, 'events'
    And header Authorization = adminAuthHeader
    And request { title: '#(titleLate)', description: 'Late event', location: 'Hall', startTime: '#(startLate)', capacity: 10 }
    When method post
    Then status 201
    And match response.event.clubCategory == categoryB

    Given path '/api/events'
    When method get
    Then status 200
    * def idxEarly = response.findIndex(x => x.title == titleEarly)
    * def idxLate = response.findIndex(x => x.title == titleLate)
    And match idxEarly != -1
    And match idxLate != -1
    And match idxEarly < idxLate
    * def earlyEvent = response[idxEarly]
    * def lateEvent = response[idxLate]
    And match earlyEvent.clubCategory == categoryA
    And match lateEvent.clubCategory == categoryB
