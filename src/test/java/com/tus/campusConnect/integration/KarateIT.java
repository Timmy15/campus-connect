package com.tus.campusConnect.integration;

import com.intuit.karate.junit5.Karate;

class KarateIT extends AbstractKarateIT {

    @Karate.Test
    Karate runAll() {
        return runFeature("classpath:karate");
    }
}
