package com.example.rabbitmq;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class Producer {

    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meterRegistry;
    private final Counter messagesProducedCounter;
    private final ConcurrentMap<String, Counter> queueCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> routingKeyCounters = new ConcurrentHashMap<>();

    @Autowired
    public Producer(RabbitTemplate rabbitTemplate, MeterRegistry meterRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.meterRegistry = meterRegistry;
        this.messagesProducedCounter = Counter.builder("rabbitmq.messages.produced")
                .description("Number of messages produced")
                .register(meterRegistry);
    }

    public void sendMessage(String queueName, String message) {
        rabbitTemplate.convertAndSend(queueName, message);
        messagesProducedCounter.increment();

        // Track messages per queue
        queueCounters.computeIfAbsent(queueName, key -> 
            Counter.builder("rabbitmq.messages.produced.queue")
                .tag("queue", key)
                .description("Number of messages produced per queue")
                .register(meterRegistry)
        ).increment();
    }

    public void sendMessageToExchange(String exchangeName, String routingKey, String message) {
        // For a topic exchange, the routing key pattern determines which queue(s) receive the message
        rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
        messagesProducedCounter.increment();

        // Track messages per routing key
        routingKeyCounters.computeIfAbsent(routingKey, key -> 
            Counter.builder("rabbitmq.messages.produced.routing_key")
                .tag("exchange", exchangeName)
                .tag("routing_key", key)
                .description("Number of messages produced per routing key")
                .register(meterRegistry)
        ).increment();
    }
}
