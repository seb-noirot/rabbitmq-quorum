package com.example.rabbitmq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RabbitmqDemoApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(RabbitmqDemoApplication.class, args);
        Producer producer = context.getBean(Producer.class);
        producer.sendMessage("testQueue", "Hello, RabbitMQ!");
    }

    @Bean
    public Producer producer() {
        return new Producer();
    }
}
