package com.example.ApacheKafka.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.ApacheKafka.repository.NotificationRepository;
import com.example.ApacheKafka.service.InAppNotificationProducer;

@Controller
@RequestMapping("/api/inapp")
public class InAppNotificationController {

    private final InAppNotificationProducer producer;
    private final NotificationRepository repo;

    public InAppNotificationController(InAppNotificationProducer producer, NotificationRepository repo) {
        this.producer = producer;
        this.repo = repo;
    }

    @PostMapping("/send")
    public ResponseEntity<String> send(@RequestParam String studentId, @RequestParam String message) {
        producer.sendNotification(studentId, message);
        return ResponseEntity.accepted().body("Notification accepted");
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<?> view(@PathVariable String studentId) {
        return ResponseEntity.ok(repo.findByStudentIdOrderByCreatedAtDesc(studentId));
    }
}
