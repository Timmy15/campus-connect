package com.tus.campusconnect.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.seed")
public class SeedProperties {

    private boolean enabled;
    private SeedUser admin = new SeedUser();
    private SeedUser student = new SeedUser();

    @Data
    public static class SeedUser {
        private String email;
        private String username;
        private String fullName;
        private String password;
    }
}
