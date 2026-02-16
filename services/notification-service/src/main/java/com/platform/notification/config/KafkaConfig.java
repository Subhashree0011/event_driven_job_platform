package com.platform.notification.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for Notification Service.
 * 
 * Consumes from:
 *   - application.created (12 partitions, keyed by job_id) — triggers email/SMS/push
 *   - job.lifecycle (6 partitions, keyed by job_id) — job status change alerts
 *   - notification.retry (3 partitions, keyed by user_id) — failed delivery retries
 * 
 * Produces to:
 *   - notification.retry — routes failed notifications for retry with backoff
 * 
 * Design decisions:
 *   - Manual ack mode (RECORD) for per-message control
 *   - Small poll batch (10 records) — notification sending is I/O-heavy (SMTP)
 *   - Dedicated concurrency per topic to isolate blast radius
 *   - DefaultErrorHandler with fixed backoff before sending to retry topic
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // ──────────────────────────────── Consumer ────────────────────────────────

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10); // Small batch — SMTP is slow
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.platform.*");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Primary listener factory — used by email/SMS/push consumers.
     * Concurrency = 3 threads (notification sending is I/O-bound, not CPU-bound).
     *
     * ConsumerFactory injected via parameter — ensures Spring singleton and @NonNull.
     */
    @Bean
    @SuppressWarnings("null")
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        // 3 retries with 2s interval before giving up on a record
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(2000L, 3)));
        return factory;
    }

    /**
     * Retry listener factory — dedicated to notification.retry topic.
     * Single-threaded to prevent retry storm amplification.
     *
     * ConsumerFactory injected via parameter — ensures Spring singleton and @NonNull.
     */
    @Bean
    @SuppressWarnings("null")
    public ConcurrentKafkaListenerContainerFactory<String, Object> retryListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(1); // Single thread — retries should be slow and controlled
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(5000L, 2)));
        return factory;
    }

    // ──────────────────────────────── Producer ────────────────────────────────

    /**
     * Producer for publishing to notification.retry topic.
     * Uses acks=all for durability — we don't want to lose retry events.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    @SuppressWarnings("null")
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
