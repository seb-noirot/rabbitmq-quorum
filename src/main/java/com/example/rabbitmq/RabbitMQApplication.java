package com.example.rabbitmq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class RabbitMQApplication implements ApplicationListener<ConsumerReadyEvent> {

    @Autowired
    private Producer producer;

    @Autowired
    private ApplicationContext applicationContext;

    private static ConfigurableApplicationContext context;

    @Value("${spring.rabbitmq.shutdown.enabled:true}")
    private boolean shutdownEnabled;

    // CountDownLatch to wait for consumers to be ready
    private final CountDownLatch consumerLatch = new CountDownLatch(1);

    @Override
    public void onApplicationEvent(ConsumerReadyEvent event) {
        // Consumer is ready, count down the latch
        System.out.println("Received ConsumerReadyEvent, consumers are ready to receive messages");
        consumerLatch.countDown();
    }

    public static void main(String[] args) {
        context = SpringApplication.run(RabbitMQApplication.class, args);
    }

    @Bean
    public CommandLineRunner sendMessagesOnStartup() {
        return args -> {
            // Wait for consumers to be ready
            System.out.println("Waiting for consumers to be ready...");
            try {
                // Wait up to 30 seconds for consumers to be ready
                if (!consumerLatch.await(30, TimeUnit.SECONDS)) {
                    System.out.println("Timeout waiting for consumers to be ready. Proceeding anyway.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Interrupted while waiting for consumers to be ready");
            }
            // Send a message every 100ms for 1 second (100 messages total)
            for (int i = 0; i < 10; i++) {
                long timestamp = System.currentTimeMillis();
                System.out.println("Sending message to queue1 at: " + timestamp);
                producer.sendMessageToExchange(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY_1,
                        "Message to queue1 sent at: " + timestamp);

                try {
                    Thread.sleep(100); // Wait 100ms between messages
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Wait a bit to ensure all messages are processed
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("All messages sent.");

            // Exit the application if shutdown is enabled
            if (shutdownEnabled) {
                System.out.println("Shutting down the application...");
                if (context != null) {
                    SpringApplication.exit(context, () -> 0);
                } else if (applicationContext instanceof ConfigurableApplicationContext) {
                    // Use the autowired context if the static one is null
                    SpringApplication.exit((ConfigurableApplicationContext) applicationContext, () -> 0);
                } else {
                    System.err.println("Application context is null, cannot exit gracefully.");
                    System.exit(0);
                }
            } else {
                System.out.println("Shutdown is disabled. Application will continue running.");
            }
        };
    }
}
