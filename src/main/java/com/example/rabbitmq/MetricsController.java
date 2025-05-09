package com.example.rabbitmq;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MeterRegistry meterRegistry;

    @Autowired
    public MetricsController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/rabbitmq")
    public Map<String, Double> getRabbitMQMetrics() {
        Map<String, Double> metrics = new HashMap<>();
        
        // Get total messages produced
        meterRegistry.find("rabbitmq.messages.produced").counters()
            .forEach(counter -> metrics.put("messages.produced", counter.count()));
        
        // Get total messages consumed
        meterRegistry.find("rabbitmq.messages.consumed").counters()
            .forEach(counter -> metrics.put("messages.consumed", counter.count()));
        
        // Get messages produced per queue
        meterRegistry.find("rabbitmq.messages.produced.queue").counters()
            .forEach(counter -> {
                String queueName = counter.getId().getTag("queue");
                metrics.put("messages.produced.queue." + queueName, counter.count());
            });
        
        // Get messages consumed per queue
        meterRegistry.find("rabbitmq.messages.consumed.queue").counters()
            .forEach(counter -> {
                String queueName = counter.getId().getTag("queue");
                metrics.put("messages.consumed.queue." + queueName, counter.count());
            });
        
        // Get messages produced per routing key
        meterRegistry.find("rabbitmq.messages.produced.routing_key").counters()
            .forEach(counter -> {
                String routingKey = counter.getId().getTag("routing_key");
                String exchange = counter.getId().getTag("exchange");
                metrics.put("messages.produced.routing_key." + exchange + "." + routingKey, counter.count());
            });
        
        return metrics;
    }
}