package com.tus.campusConnect.integration;

import com.intuit.karate.junit5.Karate;

class AuthLoginKarateIT extends AbstractKarateIT {

    @Karate.Test
    Karate authLogin() {
        return runFeature("classpath:karate/auth-login.feature");
    }
}
