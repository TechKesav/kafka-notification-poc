package com.example.ApacheKafka.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class InAppNotificationProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public InAppNotificationProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    // admin -> student
    public void sendNotification(String studentId, String message) {
        System.out.println("Publishing to Kafka → " + studentId + " | " + message);
        kafkaTemplate.send("inapp_notifications", studentId + "|" + message);
    }
    // student -> admin
    public void sendAck(String studentId, String message) {
        System.out.println("Publishing ACK to Kafka → " + studentId + " | " + message);
        kafkaTemplate.send("ack_notifications", studentId + "|" + message);
    }

}

