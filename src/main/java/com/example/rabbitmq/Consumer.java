package com.example.rabbitmq;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class Consumer implements InitializingBean {

    private final MeterRegistry meterRegistry;
    private final Counter messagesConsumedCounter;
    private final ConcurrentMap<String, Counter> queueCounters = new ConcurrentHashMap<>();
    private final AtomicBoolean ready = new AtomicBoolean(false);

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    public Consumer(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.messagesConsumedCounter = Counter.builder("rabbitmq.messages.consumed")
                .description("Number of messages consumed")
                .register(meterRegistry);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Mark the consumer as ready and publish the event
        ready.set(true);
        System.out.println("Consumer is ready to receive messages");
        eventPublisher.publishEvent(new ConsumerReadyEvent(this));
    }

    /**
     * Check if the consumer is ready to receive messages
     * @return true if the consumer is ready, false otherwise
     */
    public boolean isReady() {
        return ready.get();
    }

    private void incrementQueueCounter(String queueName) {
        queueCounters.computeIfAbsent(queueName, key -> 
            Counter.builder("rabbitmq.messages.consumed.queue")
                .tag("queue", key)
                .description("Number of messages consumed per queue")
                .register(meterRegistry)
        ).increment();
    }

    public void receiveMessage(String message) {
    }

    @RabbitListener(queues = "#{queue1.name}", concurrency = "2")
    public void receiveFromQueue1(String message) {
        System.out.println("Queue1 Consumer 1 received: " + message);
        messagesConsumedCounter.increment();
        incrementQueueCounter(RabbitMQConfig.QUEUE_1_NAME);
    }

    @RabbitListener(queues = "#{queue1.name}", concurrency = "2")
    public void receiveFromQueue1Second(String message) {
        System.out.println("Queue1 Consumer 2 received: " + message);
        messagesConsumedCounter.increment();
        incrementQueueCounter(RabbitMQConfig.QUEUE_1_NAME);
    }

    @RabbitListener(queues = "#{queue1.name}", concurrency = "2")
    public void receiveFromQueue1Third(String message) {
        System.out.println("Queue1 Consumer 3 received: " + message);
        messagesConsumedCounter.increment();
        incrementQueueCounter(RabbitMQConfig.QUEUE_1_NAME);
    }

    @RabbitListener(queues = "#{queue2.name}")
    public void receiveFromQueue2(String message) {
        System.out.println("Queue2 Consumer 1 received: " + message);
        messagesConsumedCounter.increment();
        incrementQueueCounter(RabbitMQConfig.QUEUE_2_NAME);
    }

    @RabbitListener(queues = "#{queue3.name}")
    public void receiveFromQueue3(String message) {
        System.out.println("Queue3 Consumer 1 received: " + message);
        messagesConsumedCounter.increment();
        incrementQueueCounter(RabbitMQConfig.QUEUE_3_NAME);
    }
}
