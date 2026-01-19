package com.example.ApacheKafka.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ApacheKafka.entity.FailedMessage;
import com.example.ApacheKafka.repository.FailedMessageRepository;

@Service
public class RetryService {

    private final EmailService emailService;
    private final FailedMessageRepository repo;

    public RetryService(EmailService emailService, FailedMessageRepository repo) {
        this.emailService = emailService;
        this.repo = repo;
    }

    private static final int MAX_RETRIES = 3;

    @Scheduled(fixedRate = 120000) // 2 minutes
    @Transactional
    public void retryFailedMessages() {
        List<FailedMessage> failedList = repo.findBySentFalseAndRetryCountLessThan(MAX_RETRIES);
        if (failedList.isEmpty()) return;

        System.out.println("Retrying " + failedList.size() + " failed messages...");

        for (FailedMessage msg : failedList) {
            try {
                emailService.sendEmail(msg.getRecipient(), msg.getBody());
                msg.setSent(true);
                msg.setError(null);
                System.out.println(" Retry success for " + msg.getRecipient());
            } catch (Exception e) {
                msg.setRetryCount(msg.getRetryCount() + 1);
                msg.setError(e.getMessage());
                System.err.println(" Retry failed for " + msg.getRecipient());
                if (msg.getRetryCount() >= MAX_RETRIES) {
                    System.err.println(" Marking as permanently failed: " + msg.getRecipient());
                }
            }
            msg.setLastTriedAt(LocalDateTime.now());
            repo.save(msg);
        }
    }
}
