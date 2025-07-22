package com.feng.engine.demo;

import com.feng.engine.messaging.MessageListener;
import org.springframework.stereotype.Service;

/**
 * Use the self-defined annotation to mark the method as a message listener
 *
 * @since 2025/7/23
 */
@Service
public class KafkaConsumerService {
    @MessageListener(topic = "test")
    public void consume(String message) {
        System.out.println("Received message: " + message);
    }
}
