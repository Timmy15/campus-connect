# Read Me First
This application lets students browse clubs and events, register for events, and lets administrators manage clubs, events, and registrations.

# User Guide
## Register an Account
1. Open the app in your browser (single-page app served from `/`) and switch to **Register**.
2. Enter a **unique username**.
3. Enter an email address with one of the accepted domains:
   - Students: `name@student.tus.com`
   - Admins: `name@admin.tus.com`
4. Enter a password with **at least 5 characters**.
5. Submit the form. Your role is assigned automatically based on the email domain.

## Log In
1. Open the **Login** form in the app.
2. Sign in using **either your email or your username** plus your password.
3. On success, you are redirected to the dashboard for your role.

## Using the Dashboard
- The navigation shows only the items your role can access.
- Your username is shown on the dashboard so you can confirm the logged-in account.
- Admins can open the participation dashboard to view charts for registrations per event and top clubs by registrations.
- Admins can create, update, deactivate, reactivate, and delete clubs from **Manage Clubs**.
- Admins can create, update, and delete events for a club from **Manage Clubs** (Event Management section).
- Events require a future start date and a capacity greater than zero.
- Students can view active events in **Browse Events**.
- Students can search clubs/events by name, filter by category, and sort events by date in the browse pages.
- **Browse Clubs** cards are clickable to show events for that club, including remaining capacity and registration status.
- Students can register for events directly from the club event list, and admins see a message that admin accounts cannot register.
- Deactivated clubs are hidden from **Browse Clubs** until reactivated.
- Past events are hidden from browsing and club event lists.
- Student registrations show a **COMPLETED** badge for events whose date has passed.
- Student dashboard registration counts exclude past events and only show active registrations.
- Students can cancel their event registrations from **My Registrations**.
- Deactivating a club automatically cancels upcoming/ongoing registrations for that club.
- Deleting an event removes all registrations for that event.
- Deleting a club removes all events under it and their registrations (via cascade).

## Log Out
Use the **Logout** action in the navigation to clear your session and return to the in-app login view.

# Getting Started
## Prerequisites
- Java 17
- Maven 3.9+
- MySQL 8+

## Configure Database
1. Create a database named `campus_connect_db`.
2. Update the credentials in [application.properties](C:/Users/osiyo/campus-connect/src/main/resources/application.properties):
   - `spring.datasource.url`
   - `spring.datasource.username`
   - `spring.datasource.password`

## Run the Application
```bash
mvn spring-boot:run
```

The app is configured to run on port `8081` (see `server.port` in `application.properties`).

## API Documentation (Swagger UI)
Once the app is running, open:
```
http://localhost:8081/swagger-ui/index.html
```

The Swagger UI documents all endpoints. Key ones are listed below.

### Public/Authenticated (Student + Admin)
- `GET /api/clubs` (browse active clubs)
- `GET /api/events` (browse active events)
- `GET /api/user/me` (current user profile)

### Student Endpoints
- `POST /api/student/events/{eventId}/register` (register for an event)
- `GET /api/student/registrations` (list my registrations)
- `DELETE /api/student/registrations/{registrationId}` (cancel my registration)

### Auth
- `POST /api/auth/login` (log in)
- `POST /api/auth/register` (register)
- `GET /api/auth/username-available?username=` (check username availability)

### Admin Club Management
- `GET /api/admin/clubs` (list all clubs)
- `POST /api/admin/clubs` (create a club)
- `PUT /api/admin/clubs/{id}` (update a club)
- `DELETE /api/admin/clubs/{id}` (deactivate a club)
- `PUT /api/admin/clubs/{id}/activate` (activate a club)
- `DELETE /api/admin/clubs/{id}/delete` (delete a club)

### Admin Event Management
- `GET /api/admin/clubs/{clubId}/events` (list events for a club)
- `POST /api/admin/clubs/{clubId}/events` (create an event)
- `PUT /api/admin/events/{eventId}` (update an event)
- `DELETE /api/admin/events/{eventId}` (delete an event)

### Admin Registration Management
- `GET /api/admin/events/{eventId}/registrations` (list registrations for an event)
- `DELETE /api/admin/events/{eventId}/registrations/{registrationId}` (unregister a student)

### Admin Participation Reporting
- `GET /api/admin/participation` (registrations per event and top clubs by registrations)
