package com.example.rabbitmq;

import org.springframework.context.ApplicationEvent;

/**
 * Event that is published when all consumers are ready to receive messages.
 */
public class ConsumerReadyEvent extends ApplicationEvent {
    
    public ConsumerReadyEvent(Object source) {
        super(source);
    }
}