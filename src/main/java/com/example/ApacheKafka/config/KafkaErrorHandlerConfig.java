package com.example.ApacheKafka.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler errorHandler() {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(2000);
        backOff.setMultiplier(2);
        backOff.setMaxInterval(10000);

        DefaultErrorHandler handler = new DefaultErrorHandler((record, ex) -> {
            ConsumerRecord<?, ?> r = (ConsumerRecord<?, ?>) record;
            System.err.println("Error processing record: " + r.value() + " - " + ex.getMessage());
        }, backOff);

        return handler;
    }
}
