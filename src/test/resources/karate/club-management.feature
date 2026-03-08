Feature: Club management

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'

  Scenario: Admin creates a club and it appears in browse list
    Given path '/api/auth/login'
    And request { email: 'admin@admin.tus.com', password: 'Admin123' }
    When method post
    Then status 200
    * def token = response.token
    * def authHeader = 'Bearer ' + token
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def clubName = 'Robotics-' + uuid

    Given path '/api/admin/clubs'
    And header Authorization = authHeader
    And request { name: '#(clubName)', description: 'Build robots', category: 'Tech' }
    When method post
    Then status 201
    And match response.message == 'Club created successfully.'
    And match response.club.name == clubName

    Given path '/api/clubs'
    And header Authorization = authHeader
    When method get
    Then status 200
    * def names = $response[*].name
    And match names contains clubName

  Scenario: Club creation fails when name already exists
    Given path '/api/auth/login'
    And request { email: 'admin@admin.tus.com', password: 'Admin123' }
    When method post
    Then status 200
    * def token = response.token
    * def authHeader = 'Bearer ' + token
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def clubName = 'Chess-' + uuid

    Given path '/api/admin/clubs'
    And header Authorization = authHeader
    And request { name: '#(clubName)', description: 'Strategy', category: 'Games' }
    When method post
    Then status 201

    Given path '/api/admin/clubs'
    And header Authorization = authHeader
    And request { name: '#(clubName)', description: 'Strategy', category: 'Games' }
    When method post
    Then status 409
    And match response.message == 'Club already exists'

  Scenario: Admin updates a club successfully
    Given path '/api/auth/login'
    And request { email: 'admin@admin.tus.com', password: 'Admin123' }
    When method post
    Then status 200
    * def token = response.token
    * def authHeader = 'Bearer ' + token
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def clubName = 'Drama-' + uuid

    Given path '/api/admin/clubs'
    And header Authorization = authHeader
    And request { name: '#(clubName)', description: 'Performing arts', category: 'Arts' }
    When method post
    Then status 201
    * def clubId = response.club.id

    * def updatedName = clubName + '-Updated'
    Given path '/api/admin/clubs', clubId
    And header Authorization = authHeader
    And request { name: '#(updatedName)', description: 'Updated', category: 'Arts' }
    When method put
    Then status 200
    And match response.message == 'Club updated successfully.'
    And match response.club.name == updatedName

    Given path '/api/clubs'
    And header Authorization = authHeader
    When method get
    Then status 200
    * def names = $response[*].name
    And match names contains updatedName

  Scenario: Club update fails with invalid details
    Given path '/api/auth/login'
    And request { email: 'admin@admin.tus.com', password: 'Admin123' }
    When method post
    Then status 200
    * def token = response.token
    * def authHeader = 'Bearer ' + token
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def clubName = 'Hiking-' + uuid

    Given path '/api/admin/clubs'
    And header Authorization = authHeader
    And request { name: '#(clubName)', description: 'Outdoor adventures', category: 'Sports' }
    When method post
    Then status 201
    * def clubId = response.club.id

    Given path '/api/admin/clubs', clubId
    And header Authorization = authHeader
    And request { name: '   ', description: 'Updated', category: 'Sports' }
    When method put
    Then status 400
    And match response.message == 'Invalid club details.'

    Given path '/api/clubs'
    And header Authorization = authHeader
    When method get
    Then status 200
    * def names = $response[*].name
    And match names contains clubName

  Scenario: Admin deactivates a club and it is hidden from users
    Given path '/api/auth/login'
    And request { email: 'admin@admin.tus.com', password: 'Admin123' }
    When method post
    Then status 200
    * def token = response.token
    * def authHeader = 'Bearer ' + token
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def clubName = 'Coding-' + uuid

    Given path '/api/admin/clubs'
    And header Authorization = authHeader
    And request { name: '#(clubName)', description: 'Code and coffee', category: 'Tech' }
    When method post
    Then status 201
    * def clubId = response.club.id

    Given path '/api/admin/clubs', clubId
    And header Authorization = authHeader
    When method delete
    Then status 200
    And match response.message == 'Club deactivated successfully.'

    Given path '/api/clubs'
    And header Authorization = authHeader
    When method get
    Then status 200
    * def names = $response[*].name
    And match names !contains clubName

  Scenario: Student cannot access admin club endpoints
    Given path '/api/auth/login'
    And request { email: 'student@student.tus.com', password: 'Student123' }
    When method post
    Then status 200
    * def token = response.token
    * def authHeader = 'Bearer ' + token

    Given path '/api/admin/clubs'
    And header Authorization = authHeader
    When method get
    Then status 403

  Scenario: Admin list includes deactivated club
    Given path '/api/auth/login'
    And request { email: 'admin@admin.tus.com', password: 'Admin123' }
    When method post
    Then status 200
    * def token = response.token
    * def authHeader = 'Bearer ' + token
    * def uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def clubName = 'Photography-' + uuid

    Given path '/api/admin/clubs'
    And header Authorization = authHeader
    And request { name: '#(clubName)', description: 'Camera lovers', category: 'Arts' }
    When method post
    Then status 201
    * def clubId = response.club.id

    Given path '/api/admin/clubs', clubId
    And header Authorization = authHeader
    When method delete
    Then status 200

    Given path '/api/admin/clubs'
    And header Authorization = authHeader
    When method get
    Then status 200
    * def club = response.find(x => x.id == clubId)
    And match club.name == clubName
    And match club.active == false
