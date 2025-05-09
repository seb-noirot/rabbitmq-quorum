# RabbitMQ Quorum Queues Demo

This project demonstrates the use of RabbitMQ Quorum Queues in a Spring Boot application. It includes producers, consumers, and metrics collection for monitoring message flow.

## Features

- RabbitMQ integration with Spring Boot
- Quorum Queues for high availability and data safety
- Multiple producers and consumers
- Metrics collection and exposure via REST API and Prometheus
- Docker Compose setup for easy deployment

## What are Quorum Queues?

Quorum queues are a type of queue in RabbitMQ that provide higher data safety guarantees compared to classic queues through replication across multiple nodes in a cluster. They are designed for scenarios where data durability is critical.

Key features of quorum queues:
- Always durable (survive broker restarts)
- Replicated across multiple nodes for high availability
- Consistent (all replicas have the same messages)
- Support for delivery limits to prevent infinite redelivery loops

## Running the Application

### Prerequisites

- Java 21
- Docker and Docker Compose

### Steps

1. Start the RabbitMQ container:
   ```
   docker-compose up -d
   ```

2. Build and run the application:
   ```
   ./mvnw clean package
   java -jar target/rabbitmq-quorum-1.0-SNAPSHOT.jar
   ```

3. Access the RabbitMQ Management UI:
   - URL: http://localhost:15672
   - Username: user
   - Password: password

4. Access the metrics endpoint:
   - URL: http://localhost:8080/api/metrics/rabbitmq

## Architecture

The application consists of the following components:

- **RabbitMQConfig**: Configures the RabbitMQ connection, exchanges, and quorum queues
- **Producer**: Sends messages to the queues and tracks metrics
- **Consumer**: Receives messages from the queues and tracks metrics
- **MetricsController**: Exposes metrics through a REST API

## Monitoring

The application exposes metrics through:

1. Spring Boot Actuator endpoints:
   - http://localhost:8080/actuator/metrics
   - http://localhost:8080/actuator/prometheus

2. Custom metrics endpoint:
   - http://localhost:8080/api/metrics/rabbitmq

Available metrics include:
- Total messages produced
- Total messages consumed
- Messages produced per queue
- Messages consumed per queue
- Messages produced per routing key and exchange
