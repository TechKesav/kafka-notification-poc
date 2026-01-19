package com.example.ApacheKafka.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public NotificationProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendNotification(String email, String message) {
        System.out.println("Sending to Kafka → " + email + " | " + message);
        kafkaTemplate.send("email_notifications", email + "|" + message);
    }

}
