package com.tus.campusconnect.integration;

import com.intuit.karate.junit5.Karate;

class AuthRegistrationKarateIT extends AbstractKarateIT {

    @Karate.Test
    Karate authRegistration() {
        return runFeature("classpath:karate/auth-registration.feature");
    }
}
