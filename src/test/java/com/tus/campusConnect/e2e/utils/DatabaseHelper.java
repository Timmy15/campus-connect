package com.tus.campusConnect.e2e.utils;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHelper {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseHelper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void execute(String sql) {
        jdbcTemplate.execute(sql);
    }
}
