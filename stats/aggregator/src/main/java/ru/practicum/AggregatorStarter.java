package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stat.avro.EventSimilarityAvro;
import ru.practicum.ewm.stat.avro.UserActionAvro;
import ru.practicum.kafka.KafkaConfig;
import ru.practicum.kafka.KafkaTopic;
import ru.practicum.service.SimilarityService;

import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AggregatorStarter {
    private static final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(5000);
    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    private static final int COUNT_FIX_OFFSETS = 10;
    private final KafkaProducer<Long, SpecificRecordBase> producer;
    private final KafkaConsumer<Long, UserActionAvro> consumer;
    private final EnumMap<KafkaTopic, String> topics;
    private final SimilarityService similarityService;

    public AggregatorStarter(KafkaConfig kafkaConfig, SimilarityService similarityService) {
        this.topics = kafkaConfig.getTopics();
        this.producer = new KafkaProducer<>(kafkaConfig.getProducerProps());
        this.consumer = new KafkaConsumer<>(kafkaConfig.getConsumerProps());
        this.similarityService = similarityService;
    }

    public void start() {
        log.info("Запуск aggregator...");
        try {
            consumer.subscribe(List.of(topics.get(KafkaTopic.USER_ACTIONS)));
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            while (true) {
                ConsumerRecords<Long, UserActionAvro> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);
                if (records.isEmpty()) continue;

                int count = 0;
                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    log.info("Получено сообщение: topic = {}, partition = {}, offset = {}, value = {}",
                            record.topic(), record.partition(), record.offset(), record.value());
                    List<EventSimilarityAvro> eventSimilarityAvros = similarityService.updateSimilarity(record.value());

                    for (EventSimilarityAvro eventSimilarity : eventSimilarityAvros) {
                        ProducerRecord<Long, SpecificRecordBase> producerRecord = new ProducerRecord<>(
                                topics.get(KafkaTopic.EVENTS_SIMILARITY),
                                null,
                                eventSimilarity.getTimestamp().toEpochMilli(),
                                eventSimilarity.getEventA(),
                                eventSimilarity);
                        producer.send(producerRecord);
                        manageOffsets(record, count, consumer);
                        log.info("Схожесть для событий ID {} и ID {} отправлено в topic {}",
                                eventSimilarity.getEventA(), eventSimilarity.getEventB(), producerRecord.topic());
                        count++;
                    }
                }
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка обработки user actions", e);
        } finally {
            try {
                producer.flush();
                consumer.commitSync();
            } finally {
                log.info("Завершение consumer");
                consumer.close();
                log.info("Завершение producer");
                producer.close();
            }
        }
    }

    private void manageOffsets(ConsumerRecord<Long, UserActionAvro> record, int count, KafkaConsumer<Long, UserActionAvro> consumer) {
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % COUNT_FIX_OFFSETS == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Ошибка фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }
}


