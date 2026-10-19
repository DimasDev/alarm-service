package com.example.alarmservice.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/mock/notifications")
public class MockNotificationController {

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody String payload) {
        log.info("Mock receiver received notification: {}", payload);
        return ResponseEntity.ok().build();
    }
}
