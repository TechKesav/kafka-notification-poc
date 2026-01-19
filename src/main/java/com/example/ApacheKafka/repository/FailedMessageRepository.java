package com.example.ApacheKafka.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.ApacheKafka.entity.FailedMessage;

@Repository
public interface FailedMessageRepository extends JpaRepository<FailedMessage, Long> {
    List<FailedMessage> findBySentFalse();
    List<FailedMessage> findBySentFalseAndRetryCountLessThan(int retryCount);
    Optional<FailedMessage> findByRecipientAndBody(String recipient, String body);

    @Modifying
    @Transactional
    @Query("DELETE FROM FailedMessage f WHERE f.sent = true")
    void deleteAllBySentTrue();
}
