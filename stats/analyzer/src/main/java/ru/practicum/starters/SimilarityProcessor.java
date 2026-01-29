package ru.practicum.starters;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stat.avro.EventSimilarityAvro;
import ru.practicum.kafka.KafkaConfig;
import ru.practicum.kafka.KafkaTopic;
import ru.practicum.mapper.Mapper;
import ru.practicum.model.EventSimilarity;
import ru.practicum.repository.EventSimilarityRepository;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;

@Slf4j
@Component
public class SimilarityProcessor implements Runnable {
    private final KafkaConsumer<Long, EventSimilarityAvro> consumer;
    private final EnumMap<KafkaTopic, String> topics;
    private final EventSimilarityRepository repository;
    private final Duration consumeAttemptTimeout;

    public SimilarityProcessor(KafkaConfig kafkaConfig, EventSimilarityRepository repository) {
        consumer = new KafkaConsumer<>(kafkaConfig.getSimilarityConsumerProps());
        topics = kafkaConfig.getTopics();
        this.repository = repository;
        this.consumeAttemptTimeout = Duration.ofMillis(kafkaConfig.getAttemptTimeout());
    }

    @Override
    public void run() {
        consumer.subscribe(List.of(topics.get(KafkaTopic.EVENTS_SIMILARITY)));

        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            while (true) {
                ConsumerRecords<Long, EventSimilarityAvro> records = consumer.poll(consumeAttemptTimeout);
                if (records.isEmpty()) continue;

                for (ConsumerRecord<Long, EventSimilarityAvro> record : records) {
                    log.info("Запрос схожести: topic = {}, partition = {}, offset = {}, value = {}",
                            record.topic(), record.partition(), record.offset(), record.value());

                    EventSimilarity eventSimilarity = Mapper.mapToEventSimilarity(record.value());

                    repository.findByAeventIdAndBeventId(
                            eventSimilarity.getAeventId(),
                            eventSimilarity.getBeventId()).ifPresent(oldEventSimilarity ->
                            eventSimilarity.setId(oldEventSimilarity.getId()));
                    repository.save(eventSimilarity);
                    log.info("Схожесть обработана.");
                }

                consumer.commitAsync((offsets, exception) -> {
                    if (exception != null) {
                        log.warn("Ошибка фиксации оффсетов. Offset: {}", offsets, exception);
                    }
                });
            }

        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка отправки сообщений", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                log.info("Закрываем consumer");
                consumer.close();
            }
        }
    }

    public void stop() {
        consumer.wakeup();
    }
}

