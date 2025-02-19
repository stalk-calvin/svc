package com.example.auditservice.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Component
public class KafkaTopicVerifier implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(KafkaTopicVerifier.class);

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            ListTopicsResult topics = adminClient.listTopics();
            Set<String> topicNames = topics.names().get();
            logger.info("Existing Kafka topics: {}", topicNames);

            if (topicNames.contains("audit-events")) {
                logger.info("Topic 'audit-events' already exists.");
            } else {
                logger.info("Topic 'audit-events' does not exist. Attempting to create it...");
                // The NewTopic bean should have already created it, but this is a fallback
                adminClient.createTopics(Collections.singleton(new NewTopic("audit-events", 1, (short) 1)));
                logger.info("Topic 'audit-events' created.");
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to verify or create Kafka topic: {}", e.getMessage(), e);
        }
    }
}