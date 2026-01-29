package ru.practicum.kafka;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.Properties;

@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties("analyzer.kafka")
public class KafkaConfig {
    private EnumMap<KafkaTopic, String> topics = new EnumMap<>(KafkaTopic.class);
    private Properties actionConsumerProps;
    private Properties similarityConsumerProps;
    private long attemptTimeout = 500;

    @PostConstruct
    public void logConfig() {
        log.info("KafkaConfig loaded:");
        log.info("Topics: {}", topics);
        log.info("ActionConsumerProps: {}", actionConsumerProps);
        log.info("SimilarityConsumerProps: {}", similarityConsumerProps);
    }
}
