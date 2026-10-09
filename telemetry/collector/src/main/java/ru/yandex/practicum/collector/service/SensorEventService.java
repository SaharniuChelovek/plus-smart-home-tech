package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.mapper.SensorEventMapper;
import ru.yandex.practicum.collector.producer.KafkaEventProducer;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SensorEventService {

    private final SensorEventMapper mapper;
    private final KafkaEventProducer producer;

    public void collect(SensorEventProto event) {
        var avroEvent = mapper.mapToAvro(event);
        var timestamp = Instant.ofEpochSecond(event.getTimestamp().getSeconds(), event.getTimestamp().getNanos());
        producer.sendSensorEvent(event.getHubId(), timestamp, avroEvent);
    }
}