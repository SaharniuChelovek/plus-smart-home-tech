package ru.yandex.practicum.collector.producer;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final Producer<String, SpecificRecordBase> producer;

    @Value("${kafka.topics.sensors}")
    private String sensorsTopic;

    @Value("${kafka.topics.hubs}")
    private String hubsTopic;

    public void sendSensorEvent(String hubId, SpecificRecordBase event) {
        send(sensorsTopic, hubId, event);
    }

    public void sendHubEvent(String hubId, SpecificRecordBase event) {
        send(hubsTopic, hubId, event);
    }

    private void send(String topic, String key, SpecificRecordBase value) {
        producer.send(new ProducerRecord<>(topic, key, value));
    }
}