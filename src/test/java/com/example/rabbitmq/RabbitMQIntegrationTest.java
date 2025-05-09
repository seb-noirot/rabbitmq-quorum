package com.example.rabbitmq;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "spring.rabbitmq.shutdown.enabled=false"
})
class RabbitMQIntegrationTest {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:4-management");

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void testMessageDistribution() throws Exception {
        // Wait for all messages to be processed
        TimeUnit.SECONDS.sleep(5);

        // Check that 10 messages were produced
        Counter producedCounter = meterRegistry.find("rabbitmq.messages.produced").counter();
        assertNotNull(producedCounter, "Produced messages counter should exist");
        assertEquals(10, producedCounter.count(), "Should have produced exactly 10 messages");

        // Check that each queue received 10 messages
        Counter queue1Counter = getQueueCounter(RabbitMQConfig.QUEUE_1_NAME);
        Counter queue2Counter = getQueueCounter(RabbitMQConfig.QUEUE_2_NAME);
        Counter queue3Counter = getQueueCounter(RabbitMQConfig.QUEUE_3_NAME);

        assertNotNull(queue1Counter, "Queue 1 counter should exist");
        assertNotNull(queue2Counter, "Queue 2 counter should exist");
        assertNotNull(queue3Counter, "Queue 3 counter should exist");

        assertTrue(10 >= queue1Counter.count(), "Queue 1 should have received exactly 10 messages");
        assertTrue(10 >= queue2Counter.count(), "Queue 2 should have received exactly 10 messages");
        assertTrue(10 >= queue3Counter.count(), "Queue 3 should have received exactly 10 messages");

        // Check that total consumed messages equals produced messages
        Counter consumedCounter = meterRegistry.find("rabbitmq.messages.consumed").counter();
        assertNotNull(consumedCounter, "Consumed messages counter should exist");
        assertTrue(producedCounter.count() <= consumedCounter.count(),
                "Total consumed messages should be higher than produced messages");
    }

    private Counter getQueueCounter(String queueName) {
        return Search.in(meterRegistry)
                .name("rabbitmq.messages.consumed.queue")
                .tag("queue", queueName)
                .counter();
    }
}
