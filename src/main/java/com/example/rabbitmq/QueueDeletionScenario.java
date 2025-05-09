package com.example.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class demonstrates a scenario where a queue is deleted while messages are being produced and consumed.
 */
@Configuration
@EnableScheduling
public class QueueDeletionScenario {

    public static final String DELETION_QUEUE_NAME = "deletion-queue";
    public static final String DELETION_EXCHANGE_NAME = "deletion-exchange";
    public static final String DELETION_ROUTING_KEY = "deletion-key";

    @Bean(name = "deletionScenarioRabbitAdmin")
    public RabbitAdmin deletionScenarioRabbitAdmin(ConnectionFactory connectionFactory) {
        System.out.println("Creating RabbitAdmin for deletion scenario");
        return new RabbitAdmin(connectionFactory);
    }

    @Bean(name = "deletionQueue")
    public Queue deletionQueue(@Qualifier("deletionScenarioRabbitAdmin") RabbitAdmin rabbitAdmin) {
        System.out.println("Creating deletion queue");
        Queue queue = QueueBuilder.durable(DELETION_QUEUE_NAME)
                .autoDelete()
                .build();
        queue.setAdminsThatShouldDeclare(rabbitAdmin);
        return queue;
    }

    @Bean(name = "deletionExchange")
    public DirectExchange deletionExchange(@Qualifier("deletionScenarioRabbitAdmin") RabbitAdmin rabbitAdmin) {
        System.out.println("Creating deletion exchange");
        DirectExchange exchange = ExchangeBuilder.directExchange(DELETION_EXCHANGE_NAME)
                .durable(true)
                .build();
        exchange.setAdminsThatShouldDeclare(rabbitAdmin);
        return exchange;
    }

    @Bean(name = "deletionBinding")
    public Binding deletionBinding(@Qualifier("deletionQueue") Queue deletionQueue, 
                                  @Qualifier("deletionExchange") DirectExchange deletionExchange) {
        System.out.println("Creating deletion binding");
        return BindingBuilder.bind(deletionQueue)
                .to(deletionExchange)
                .with(DELETION_ROUTING_KEY);
    }

    @Bean(name = "deletionContainer")
    public SimpleMessageListenerContainer deletionContainer(
            ConnectionFactory connectionFactory,
            DeletionConsumer deletionConsumer) {
        System.out.println("Creating deletion container");
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(DELETION_QUEUE_NAME);
        container.setMessageListener(new MessageListenerAdapter(deletionConsumer, "receiveMessage"));
        return container;
    }

    /**
     * Producer that continuously sends messages to the deletion queue.
     */
    @Component
    public static class DeletionProducer {
        private final RabbitTemplate rabbitTemplate;
        private final AtomicInteger messageCount = new AtomicInteger(0);
        private final RabbitAdmin rabbitAdmin;
        private final Queue deletionQueue;
        private final DirectExchange deletionExchange;
        private final Binding deletionBinding;

        @Autowired
        public DeletionProducer(
                RabbitTemplate rabbitTemplate,
                @Qualifier("deletionScenarioRabbitAdmin") RabbitAdmin rabbitAdmin,
                @Qualifier("deletionQueue") Queue deletionQueue,
                @Qualifier("deletionExchange") DirectExchange deletionExchange,
                @Qualifier("deletionBinding") Binding deletionBinding) {
            this.rabbitTemplate = rabbitTemplate;
            this.rabbitAdmin = rabbitAdmin;
            this.deletionQueue = deletionQueue;
            this.deletionExchange = deletionExchange;
            this.deletionBinding = deletionBinding;
        }

        private boolean queueExists() {
            return rabbitAdmin.getQueueProperties(DELETION_QUEUE_NAME) != null;
        }

        private void recreateQueueIfNeeded() {
            if (!queueExists()) {
                System.out.println("Queue " + DELETION_QUEUE_NAME + " does not exist. Recreating...");
                rabbitAdmin.declareQueue(deletionQueue);
                rabbitAdmin.declareExchange(deletionExchange);
                rabbitAdmin.declareBinding(deletionBinding);
                System.out.println("Queue " + DELETION_QUEUE_NAME + " has been recreated.");
            }
        }

        @Scheduled(fixedRate = 500) // Send a message every 500ms
        public void sendMessage() {
            int count = messageCount.incrementAndGet();
            String message = "Deletion scenario message #" + count;
            System.out.println("Sending: " + message);
            try {
                // Check if queue exists and recreate if needed
                recreateQueueIfNeeded();
                rabbitTemplate.convertAndSend(DELETION_EXCHANGE_NAME, DELETION_ROUTING_KEY, message);
            } catch (Exception e) {
                System.out.println("Failed to send message: " + e.getMessage());
            }
        }

        public int getMessageCount() {
            return messageCount.get();
        }
    }

    /**
     * Consumer that processes messages from the deletion queue.
     */
    @Component
    public static class DeletionConsumer {
        private final AtomicInteger receivedCount = new AtomicInteger(0);

        public void receiveMessage(String message) {
            int count = receivedCount.incrementAndGet();
            System.out.println("Received: " + message + " (total received: " + count + ")");
        }

        public int getReceivedCount() {
            return receivedCount.get();
        }
    }

    /**
     * Scheduler that deletes the queue after a certain time.
     */
    @Component
    public static class QueueDeletionScheduler {
        private final RabbitAdmin rabbitAdmin;
        private boolean queueDeleted = false;

        @Autowired
        public QueueDeletionScheduler(@Qualifier("deletionScenarioRabbitAdmin") RabbitAdmin rabbitAdmin) {
            System.out.println("Creating QueueDeletionScheduler with RabbitAdmin: " + rabbitAdmin);
            this.rabbitAdmin = rabbitAdmin;
        }

        @Scheduled(initialDelay = 5000, fixedDelay = Long.MAX_VALUE) // Delete queue after 5 seconds
        public void deleteQueue() {
            if (!queueDeleted) {
                System.out.println("Deleting queue: " + DELETION_QUEUE_NAME);
                rabbitAdmin.deleteQueue(DELETION_QUEUE_NAME);
                queueDeleted = true;
                System.out.println("Queue deleted: " + DELETION_QUEUE_NAME);
            }
        }

        public boolean isQueueDeleted() {
            return queueDeleted;
        }
    }
}
