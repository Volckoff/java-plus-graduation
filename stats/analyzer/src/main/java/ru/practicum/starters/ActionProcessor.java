package ru.practicum.starters;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stat.avro.UserActionAvro;
import ru.practicum.kafka.KafkaConfig;
import ru.practicum.kafka.KafkaTopic;
import ru.practicum.model.ActionType;
import ru.practicum.service.ActionService;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;

@Slf4j
@Component
public class ActionProcessor implements Runnable {
    private final KafkaConsumer<Long, UserActionAvro> consumer;
    private final EnumMap<KafkaTopic, String> topics;
    private final ActionService actionService;
    private final Duration consumeAttemptTimeout;

    public ActionProcessor(KafkaConfig kafkaConfig, ActionService actionService) {
        consumer = new KafkaConsumer<>(kafkaConfig.getActionConsumerProps());
        topics = kafkaConfig.getTopics();
        this.actionService = actionService;
        this.consumeAttemptTimeout = Duration.ofMillis(kafkaConfig.getAttemptTimeout());
    }

    @Override
    public void run() {
        log.info("Запуск ActionProcessor.");
        consumer.subscribe(List.of(topics.get(KafkaTopic.USER_ACTIONS)));

        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            while (true) {
                ConsumerRecords<Long, UserActionAvro> records = consumer.poll(consumeAttemptTimeout);
                if (records.isEmpty()) continue;

                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    log.info("Запрос действий пользователя: topic = {}, partition = {}, offset = {}, value = {}",
                            record.topic(), record.partition(), record.offset(), record.value());

                    actionService.addAction(record.value().getUserId(), record.value().getEventId(),
                            ActionType.valueOf(record.value().getActionType().name()),
                            record.value().getTimestamp());

                    log.info("Действия обработаны.");
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

