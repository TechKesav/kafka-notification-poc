package com.example.ApacheKafka.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ApacheKafka.service.NotificationProducer;
import com.example.ApacheKafka.util.ExcelReader;

@RestController
@RequestMapping("/notify")
public class NotificationController {

    private final NotificationProducer producer;

    public NotificationController(NotificationProducer producer) {
        this.producer = producer;
    }


    @GetMapping("/emails")
    public String notifyAllUsers() {
        List<String> emails = ExcelReader.readEmails("users.xlsx");
        for (String email : emails) {
            producer.sendNotification(email, "Hello from Kafka Notification Demo!");
        }
        return "Notifications accepted for processing";
    }

    // New endpoint for sending email notification to a single email
    @org.springframework.web.bind.annotation.PostMapping("/email")
    public String sendEmailNotification(
            @org.springframework.web.bind.annotation.RequestParam String email,
            @org.springframework.web.bind.annotation.RequestParam String message) {
        producer.sendNotification(email, message);
        return "Email notification accepted for " + email;
    }

}
