package com.example.rabbitmq;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "spring.rabbitmq.shutdown.enabled=false"
})
class QueueDeletionTest {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:4-management");

    @Autowired
    private QueueDeletionScenario.DeletionProducer producer;

    @Autowired
    private QueueDeletionScenario.DeletionConsumer consumer;

    @Autowired
    private QueueDeletionScenario.QueueDeletionScheduler scheduler;

    @Test
    void testQueueDeletionScenario() throws Exception {
        // Wait for initial messages to be produced and consumed
        TimeUnit.SECONDS.sleep(3);

        // Verify that messages are being produced and consumed
        int producedBefore = producer.getMessageCount();
        int consumedBefore = consumer.getReceivedCount();

        System.out.println("Before queue deletion - Produced: " + producedBefore + ", Consumed: " + consumedBefore);

        assertTrue(producedBefore > 0, "Should have produced some messages before queue deletion");
        assertTrue(consumedBefore > 0, "Should have consumed some messages before queue deletion");

        // Wait for the queue to be deleted (scheduler deletes after 5 seconds)
        TimeUnit.SECONDS.sleep(5);

        // Verify that the queue was deleted
        assertTrue(scheduler.isQueueDeleted(), "Queue should have been deleted");

        // Wait for more messages to be produced and for the queue to be recreated
        TimeUnit.SECONDS.sleep(3);

        // Verify that messages are still being produced
        int producedAfterDeletion = producer.getMessageCount();
        int consumedAfterDeletion = consumer.getReceivedCount();

        System.out.println("Immediately after queue deletion - Produced: " + producedAfterDeletion + 
                          ", Consumed: " + consumedAfterDeletion);

        assertTrue(producedAfterDeletion > producedBefore, 
                  "Should have produced more messages after queue deletion");

        // Wait for the queue to be recreated and messages to be consumed
        TimeUnit.SECONDS.sleep(5);

        // Verify that messages are now being consumed again after queue recreation
        int producedAfterRecreation = producer.getMessageCount();
        int consumedAfterRecreation = consumer.getReceivedCount();

        System.out.println("After queue recreation - Produced: " + producedAfterRecreation + 
                          ", Consumed: " + consumedAfterRecreation);

        assertTrue(producedAfterRecreation > producedAfterDeletion, 
                  "Should have produced more messages after queue recreation");
        assertTrue(consumedAfterRecreation > consumedAfterDeletion, 
                  "Should have consumed more messages after queue recreation");
    }
}
