package com.ges.boutique.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationPersistanceService service;

    @GetMapping
    public ResponseEntity<List<Notification>> getTout() {
        return ResponseEntity.ok(service.getTout());
    }

    @GetMapping("/non-lues")
    public ResponseEntity<List<Notification>> getNonLues() {
        return ResponseEntity.ok(service.getNonLues());
    }

    @GetMapping("/non-lues/{userId}")
    public ResponseEntity<List<Notification>> getNonLuesParUser(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getNonLues());
    }

    @GetMapping("/non-lues/{userId}/map")
    public ResponseEntity<List<Notification>> getNonLuesMap(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getNonLues());
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> count() {
        return ResponseEntity.ok(Map.of("count", service.countNonLues()));
    }

    @PutMapping("/lu/{id}")
    public ResponseEntity<Notification> marquerLue(@PathVariable Long id) {
        return ResponseEntity.ok(service.marquerLue(id));
    }

    @PutMapping("/lire/{id}")
    public ResponseEntity<Notification> marquerLueAlt(@PathVariable Long id) {
        return ResponseEntity.ok(service.marquerLue(id));
    }

    @PutMapping("/tout-lire")
    public ResponseEntity<Void> marquerToutesLues() {
        service.marquerToutesLues();
        return ResponseEntity.ok().build();
    }
}
