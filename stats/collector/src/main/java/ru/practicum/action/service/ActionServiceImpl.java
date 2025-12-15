package ru.practicum.action.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.practicum.action.mapper.UserActionMapper;
import ru.practicum.action.model.UserAction;
import ru.practicum.ewm.stat.avro.UserActionAvro;
import ru.practicum.kafka.KafkaConfig;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActionServiceImpl implements ActionService {
    private final Producer<Long, SpecificRecordBase> producer;
    private final KafkaConfig kafkaConfig;

    @Override
    public void collectUserAction(UserAction userAction) {
        log.info("ActionService: вызов collectUserAction для UserAction = {}", userAction);
        UserActionAvro userActionAvro = UserActionMapper.toUserActionAvro(userAction);
        send(kafkaConfig.getKafkaProperties().getUserActionTopic(),
                userActionAvro.getEventId(),
                userAction.getTimestamp().toEpochMilli(),
                userActionAvro);
    }

    private void send(String topic, Long key, Long timestamp, SpecificRecordBase specificRecordBase) {
        ProducerRecord<Long, SpecificRecordBase> rec = new ProducerRecord<>(
                topic,
                null,
                timestamp,
                key,
                specificRecordBase);
        producer.send(rec, (metadata, exception) -> {
            if (exception != null) {
                log.error("Kafka: сообщение не отправлено, topic: {}", topic, exception);
            } else {
                log.info("Kafka: сообщение отправлено, topic: {}, partition: {}, offset: {}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }

    @PreDestroy
    private void close() {
        if (producer != null) {
            producer.flush();
            producer.close();
        }
    }
}
