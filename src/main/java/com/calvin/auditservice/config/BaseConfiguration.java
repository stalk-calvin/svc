package com.calvin.auditservice.config;

import com.google.gson.Gson;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class BaseConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topic.audit}")
    private String auditLogsTopic;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        KafkaAdmin kafkaAdmin = new KafkaAdmin(configs);
        kafkaAdmin.setFatalIfBrokerNotAvailable(true); // Fail fast if Kafka is not available
        return kafkaAdmin;
    }

    @Bean
    @DependsOn("kafkaAdmin") // Ensure KafkaAdmin is initialized first
    public NewTopic auditLogsTopic() {
        return new NewTopic(auditLogsTopic, 1, (short) 1); // 1 partition, 1 replica
    }

    @Bean
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(5) // Retry up to 5 times
                .fixedBackoff(1000) // Wait 1 second between retries
                .build();
    }

    @Bean
    public Gson gson() {
        return new Gson();
    }
}
