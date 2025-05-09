package com.example.rabbitmq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class Consumer {

    @RabbitListener(queues = "testQueue")
    public void receiveMessage(String message) {
        System.out.println("Received message: " + message);
    }
}
