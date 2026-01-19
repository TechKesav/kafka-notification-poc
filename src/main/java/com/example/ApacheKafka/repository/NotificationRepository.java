package com.example.ApacheKafka.repository;

import com.example.ApacheKafka.entity.NotificationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationMessage, Long> {
    List<NotificationMessage> findByStudentIdOrderByCreatedAtDesc(String studentId);
}

