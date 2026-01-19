package com.example.ApacheKafka.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "failed_message", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"recipient", "body"})
})
public class FailedMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String recipient;
    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(columnDefinition = "TEXT")
    private String body;

    private boolean sent = false;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastTriedAt;

    private int retryCount = 0;

    public FailedMessage(Long id, String recipient, String body, String error, boolean sent, LocalDateTime createdAt, LocalDateTime lastTriedAt) {
        this.id = id;
        this.recipient = recipient;
        this.body = body;
        this.error = error;
        this.sent = sent;
        this.createdAt = createdAt;
        this.lastTriedAt = lastTriedAt;
        this.retryCount = 0;
    }

    public FailedMessage() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastTriedAt() {
        return lastTriedAt;
    }

    public void setLastTriedAt(LocalDateTime lastTriedAt) {
        this.lastTriedAt = lastTriedAt;
    }
}
