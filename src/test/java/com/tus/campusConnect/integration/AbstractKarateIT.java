package com.tus.campusConnect.integration;

import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractKarateIT {

    @LocalServerPort
    private int port;

    protected Karate runFeature(String featurePath) {
        return Karate.run(featurePath)
                .systemProperty("karate.baseUrl", "http://localhost:" + port);
    }
}
