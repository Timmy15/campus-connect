package com.tus.campusconnect.testsupport;

public final class TestUsers {

    private static final TestUsers INSTANCE = new TestUsers();

    private final String adminEmail = "admin@admin.tus.com";
    private final String adminPassword = "Admin123";
    private final String adminUsername = "admin";

    private final String studentEmail = "student@student.tus.com";
    private final String studentPassword = "Student123";
    private final String studentUsername = "student";

    private final String invalidEmail = "invalid@student.tus.com";
    private final String wrongPassword = "WrongPass";

    private TestUsers() {
    }

    public static TestUsers getInstance() {
        return INSTANCE;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public String getStudentPassword() {
        return studentPassword;
    }

    public String getStudentUsername() {
        return studentUsername;
    }

    public String getInvalidEmail() {
        return invalidEmail;
    }

    public String getWrongPassword() {
        return wrongPassword;
    }
}
