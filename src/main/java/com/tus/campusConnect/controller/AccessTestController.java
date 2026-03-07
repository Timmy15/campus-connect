package com.tus.campusConnect.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccessTestController {

    @GetMapping("/api/admin/ping")
    public ResponseEntity<String> adminPing() {
        return ResponseEntity.ok("admin-ok");
    }

    @GetMapping("/api/student/ping")
    public ResponseEntity<String> studentPing() {
        return ResponseEntity.ok("student-ok");
    }
}
