package ru.practicum.starters;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stat.avro.EventSimilarityAvro;
import ru.practicum.kafka.KafkaConfig;
import ru.practicum.mapper.Mapper;
import ru.practicum.model.EventSimilarity;
import ru.practicum.repository.EventSimilarityRepository;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class EventSimilarityStarter implements Runnable {
    private final Consumer<Long, EventSimilarityAvro> eventSimilarityConsumer;
    private final KafkaConfig kafkaConfig;
    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    private final EventSimilarityRepository eventSimilarityRepository;

    public EventSimilarityStarter(@Qualifier("getEventSimilarityConsumer") Consumer<Long, EventSimilarityAvro> eventSimilarityConsumer,
                                  KafkaConfig kafkaConfig,
                                  EventSimilarityRepository eventSimilarityRepository) {
        this.eventSimilarityConsumer = eventSimilarityConsumer;
        this.kafkaConfig = kafkaConfig;
        this.eventSimilarityRepository = eventSimilarityRepository;
    }

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(eventSimilarityConsumer::wakeup));
        try {
            eventSimilarityConsumer.subscribe(List.of(kafkaConfig.getKafkaProperties().getEventsSimilarityTopic()));
            while (true) {
                ConsumerRecords<Long, EventSimilarityAvro> records = eventSimilarityConsumer
                        .poll(Duration.ofMillis(kafkaConfig.getKafkaProperties()
                                .getEventSimilarityConsumer().getAttemptTimeout()));
                int count = 0;
                for (ConsumerRecord<Long, EventSimilarityAvro> record : records) {
                    handleRecord(record);
                    manageOffsets(record, count, eventSimilarityConsumer);
                    count++;
                }
                eventSimilarityConsumer.commitAsync();
            }

        } catch (WakeupException ignores) {
            // игнорируем - закрываем консьюмер и продюсер в блоке finally
        } catch (Exception e) {
            log.error("Ошибка во время обработки события схожести", e);
        } finally {

            try {
                eventSimilarityConsumer.commitSync(currentOffsets);

            } finally {
                log.info("Закрываем consumer");
                eventSimilarityConsumer.close();
            }
        }
    }

    private void handleRecord(ConsumerRecord<Long, EventSimilarityAvro> consumerRecord) throws InterruptedException {
        log.info("Запись {}", consumerRecord);
        EventSimilarity eventSimilarity = Mapper.mapToEventSimilarity(consumerRecord.value());

        eventSimilarityRepository.findByAeventIdAndBeventId(
                eventSimilarity.getAeventId(),
                eventSimilarity.getBeventId()).ifPresent(oldEventSimilarity ->
                eventSimilarity.setId(oldEventSimilarity.getId()));
        eventSimilarityRepository.save(eventSimilarity);
    }

    private void manageOffsets(ConsumerRecord<Long, EventSimilarityAvro> consumerRecord,
                               int count,
                               Consumer<Long, EventSimilarityAvro> consumer) {
        currentOffsets.put(
                new TopicPartition(consumerRecord.topic(), consumerRecord.partition()),
                new OffsetAndMetadata(consumerRecord.offset() + 1)
        );

        if (count % 10 == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Ошибка во время фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }
}

