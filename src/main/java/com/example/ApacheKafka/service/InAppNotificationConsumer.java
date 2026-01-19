package com.example.ApacheKafka.service;

import com.example.ApacheKafka.entity.NotificationMessage;
import com.example.ApacheKafka.repository.NotificationRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class InAppNotificationConsumer {

    private final NotificationRepository repo;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public InAppNotificationConsumer(NotificationRepository repo, SimpMessagingTemplate messagingTemplate) {
        this.repo = repo;
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "inapp_notifications", groupId = "inapp-group")
    public void listen(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            String[] parts = record.value().split("\\|", 2);
            if (parts.length < 2) {
                ack.acknowledge();
                return;
            }

            String studentId = parts[0];
            String message = parts[1];

            NotificationMessage notification = new NotificationMessage();
            notification.setStudentId(studentId);
            notification.setMessage(message);
            notification.setSent(true);
            notification.setCreatedAt(LocalDateTime.now());

            repo.save(notification);

            messagingTemplate.convertAndSend("/topic/notifications/" + studentId, notification);

            System.out.println("📨 Sent WebSocket notification to student: " + studentId);
        } finally {
            ack.acknowledge();
        }
    }
    @KafkaListener(topics = "ack_notifications", groupId = "ack-group")
    public void listenAck(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            String[] parts = record.value().split("\\|", 2);
            if (parts.length < 2) {
                ack.acknowledge();
                return;
            }

            String studentId = parts[0];
            String message = parts[1];

            // Send this ACK to admin WebSocket topic
            messagingTemplate.convertAndSend("/topic/admin/acks",
                    String.format("{\"studentId\":\"%s\", \"message\":\"%s\"}", studentId, message));

            System.out.println("✅ ACK received from student " + studentId + ": " + message);
        } finally {
            ack.acknowledge();
        }
    }
}
