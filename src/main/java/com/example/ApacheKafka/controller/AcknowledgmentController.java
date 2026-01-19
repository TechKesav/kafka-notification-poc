package com.example.ApacheKafka.controller;

import com.example.ApacheKafka.entity.NotificationMessage;
import com.example.ApacheKafka.service.InAppNotificationProducer;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class AcknowledgmentController {

    private final InAppNotificationProducer producer;

    public AcknowledgmentController(InAppNotificationProducer producer) {
        this.producer = producer;
    }

    @MessageMapping("/ack")
    public void receiveAck(@Payload NotificationMessage ackMessage) {
        System.out.println("✅ Student ACK received via WebSocket → " + ackMessage.getStudentId() + ": " + ackMessage.getMessage());
        producer.sendAck(ackMessage.getStudentId(), ackMessage.getMessage());
    }
}
