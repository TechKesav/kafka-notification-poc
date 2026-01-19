package com.example.ApacheKafka.service;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.example.ApacheKafka.entity.FailedMessage;
import com.example.ApacheKafka.repository.FailedMessageRepository;

@Service
public class NotificationConsumer {

    private final JavaMailSender mailSender;
    private final Executor emailExecutor;
    private final FailedMessageRepository repo;

    public NotificationConsumer(JavaMailSender mailSender,
                                @Qualifier("applicationTaskExecutor") Executor emailExecutor,
                                FailedMessageRepository repo) {
        this.mailSender = mailSender;
        this.emailExecutor = emailExecutor;
        this.repo = repo;
    }

    @KafkaListener(topics = "email_notifications", groupId = "email-group", concurrency = "2")
    public void listen(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String message = record.value();
        String[] parts = message.split("\\|", 2);

        if (parts.length < 2) {
            ack.acknowledge();
            return;
        }

        String to = parts[0].trim();
        String body = parts[1];

        emailExecutor.execute(() -> {
            int maxRetries = 3;
            int attempt = 0;
            long[] backoff = {60000, 300000, 900000}; // 1 min, 5 min, 15 min in ms
            boolean sent = false;
            Exception lastException = null;

            while (attempt < maxRetries && !sent) {
                try {
                    SimpleMailMessage mail = new SimpleMailMessage();
                    mail.setTo(to);
                    mail.setSubject("Kafka Notification");
                    mail.setText(body);
                    mailSender.send(mail);
                    sent = true;
                } catch (Exception e) {
                    lastException = e;
                    try {
                        Thread.sleep(backoff[attempt]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                attempt++;
            }

            FailedMessage failed = repo.findByRecipientAndBody(to, body)
                    .orElseGet(FailedMessage::new);
            failed.setRecipient(to);
            failed.setBody(body);
            failed.setLastTriedAt(LocalDateTime.now());
            if (failed.getCreatedAt() == null) failed.setCreatedAt(LocalDateTime.now());

            if (sent) {
                failed.setSent(true);
                failed.setError(null);
                repo.save(failed);
                System.out.println("Email sent successfully to " + to);
            } else {
                failed.setSent(false);
                failed.setError(lastException != null ? lastException.getMessage() : "Unknown error");
                repo.save(failed);
                System.out.println("Failed to send email to " + to + " after retries: " + (lastException != null ? lastException.getMessage() : "Unknown error"));
            }
        });
        ack.acknowledge();
    }
}
