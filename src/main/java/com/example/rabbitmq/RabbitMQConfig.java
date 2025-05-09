package com.example.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration class that sets up quorum queues and their bindings.
 * 
 * Quorum queues provide higher data safety guarantees compared to classic queues
 * through replication across multiple nodes in a cluster. They are designed for
 * scenarios where data durability is critical.
 * 
 * Key features of quorum queues:
 * - Always durable (survive broker restarts)
 * - Replicated across multiple nodes for high availability
 * - Consistent (all replicas have the same messages)
 * - Support for delivery limits to prevent infinite redelivery loops
 */
@Configuration
public class RabbitMQConfig {

    static final String EXCHANGE_NAME = "topicExchange";
    static final String QUEUE_1_NAME = "queue1";
    static final String QUEUE_2_NAME = "queue2";
    static final String QUEUE_3_NAME = "queue3";

    static final String ROUTING_KEY_1 = "#";
    static final String ROUTING_KEY_2 = "#";
    static final String ROUTING_KEY_3 = "#";

    @Bean(name = "quorumRabbitAdmin")
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.afterPropertiesSet();
        return rabbitAdmin;
    }

    @Bean
    TopicExchange topicExchange(@Qualifier("quorumRabbitAdmin") AmqpAdmin amqpAdmin) {
        return ExchangeBuilder.topicExchange(EXCHANGE_NAME)
                .durable(true)
                .admins(amqpAdmin)
                .build();
    }

    @Bean
    Queue queue1(@Qualifier("quorumRabbitAdmin")AmqpAdmin amqpAdmin) {
        return createQueue(amqpAdmin, QUEUE_1_NAME);
    }

    @Bean
    Queue queue2(@Qualifier("quorumRabbitAdmin")AmqpAdmin amqpAdmin) {
        return createQueue(amqpAdmin, QUEUE_2_NAME);
    }

    @Bean
    Queue queue3(@Qualifier("quorumRabbitAdmin")AmqpAdmin amqpAdmin) {
        return createQueue(amqpAdmin, QUEUE_3_NAME);
    }

    @Bean
    Binding binding1(TopicExchange topicExchange, @Qualifier("queue1") Queue queue) {
        return BindingBuilder.bind(queue).to(topicExchange).with(ROUTING_KEY_1);
    }

    @Bean
    Binding binding2(TopicExchange topicExchange, @Qualifier("queue2") Queue queue) {
        return BindingBuilder.bind(queue).to(topicExchange).with(ROUTING_KEY_2);
    }

    @Bean
    Binding binding3(TopicExchange topicExchange, @Qualifier("queue3") Queue queue) {
        return BindingBuilder.bind(queue).to(topicExchange).with(ROUTING_KEY_3);
    }

    @Bean
    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
                                             MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(QUEUE_1_NAME, QUEUE_2_NAME, QUEUE_3_NAME);
        container.setMessageListener(listenerAdapter);
        return container;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(Consumer consumer) {
        return new MessageListenerAdapter(consumer, "receiveMessage");
    }

    private Queue createQueue(AmqpAdmin amqpAdmin, String name) {
        Queue queue = QueueBuilder.durable(name)
                .maxLengthBytes(1000000)
                .autoDelete()
                .build();
        queue.setAdminsThatShouldDeclare(amqpAdmin);
        return queue;
    }
}
