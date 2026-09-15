package ru.yandex.practicum.collector.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final Producer<String, SpecificRecordBase> producer;

    @Value("${kafka.topics.sensors}")
    private String sensorsTopic;

    @Value("${kafka.topics.hubs}")
    private String hubsTopic;

    public void sendSensorEvent(String hubId, Instant timestamp, SpecificRecordBase event) {
        send(sensorsTopic, hubId, timestamp, event);
    }

    public void sendHubEvent(String hubId, Instant timestamp, SpecificRecordBase event) {
        send(hubsTopic, hubId, timestamp, event);
    }

    private void send(String topic, String key, Instant timestamp, SpecificRecordBase value) {
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(
                topic,
                null,
                timestamp.toEpochMilli(),
                key,
                value
        );

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Ошибка отправки события в топик {}: {}", topic, exception.getMessage(), exception);
            } else {
                log.debug("Событие отправлено в топик {} (partition={}, offset={})",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }
}