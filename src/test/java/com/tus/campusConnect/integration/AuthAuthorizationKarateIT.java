package com.tus.campusConnect.integration;

import com.intuit.karate.junit5.Karate;

class AuthAuthorizationKarateIT extends AbstractKarateIT {

    @Karate.Test
    Karate authAuthorization() {
        return runFeature("classpath:karate/auth-authorization.feature");
    }
}
