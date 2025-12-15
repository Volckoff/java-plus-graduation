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
import ru.practicum.ewm.stat.avro.UserActionAvro;
import ru.practicum.kafka.KafkaConfig;
import ru.practicum.service.RecommendationService;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class UserActionStarter implements Runnable {
    private final Consumer<Long, UserActionAvro> userActionConsumer;
    private final KafkaConfig kafkaConfig;
    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    private final RecommendationService recommendationService;

    public UserActionStarter(@Qualifier("getUserActionConsumer") Consumer<Long, UserActionAvro> userActionConsumer,
                             KafkaConfig kafkaConfig,
                             RecommendationService recommendationService) {
        this.userActionConsumer = userActionConsumer;
        this.kafkaConfig = kafkaConfig;
        this.recommendationService = recommendationService;
    }

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(userActionConsumer::wakeup));
        try {
            userActionConsumer.subscribe(List.of(kafkaConfig.getKafkaProperties().getUserActionTopic()));
            while (true) {
                ConsumerRecords<Long, UserActionAvro> records = userActionConsumer
                        .poll(Duration.ofMillis(kafkaConfig.getKafkaProperties()
                                .getUserActionConsumer().getAttemptTimeout()));
                int count = 0;
                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    handleRecord(record);
                    manageOffsets(record, count, userActionConsumer);
                    count++;
                }
                userActionConsumer.commitAsync();
            }

        } catch (WakeupException ignores) {
        } catch (Exception e) {
            log.error("Ошибка во время обработки события пользователя", e);
        } finally {

            try {
                userActionConsumer.commitSync(currentOffsets);

            } finally {
                log.info("Закрываем consumer");
                userActionConsumer.close();
            }
        }
    }

    private void handleRecord(ConsumerRecord<Long, UserActionAvro> consumerRecord) throws InterruptedException {
        log.info("Запись {}", consumerRecord);
        recommendationService.saveUserAction(consumerRecord.value());
    }

    private void manageOffsets(ConsumerRecord<Long, UserActionAvro> consumerRecord,
                               int count,
                               Consumer<Long, UserActionAvro> consumer) {
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

