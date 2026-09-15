package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.mapper.SensorEventMapper;
import ru.yandex.practicum.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.collector.producer.KafkaEventProducer;

@Service
@RequiredArgsConstructor
public class SensorEventService {

    private final SensorEventMapper mapper;
    private final KafkaEventProducer producer;

    public void collect(SensorEvent event) {
        var avroEvent = mapper.mapToAvro(event);
        producer.sendSensorEvent(event.getHubId(), event.getTimestamp(), avroEvent);
    }
}