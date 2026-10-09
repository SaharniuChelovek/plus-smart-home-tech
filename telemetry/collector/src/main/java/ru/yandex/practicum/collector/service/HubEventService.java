package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.mapper.HubEventMapper;
import ru.yandex.practicum.collector.producer.KafkaEventProducer;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class HubEventService {

    private final HubEventMapper mapper;
    private final KafkaEventProducer producer;

    public void collect(HubEventProto event) {
        var avroEvent = mapper.mapToAvro(event);
        var timestamp = Instant.ofEpochSecond(event.getTimestamp().getSeconds(), event.getTimestamp().getNanos());
        producer.sendHubEvent(event.getHubId(), timestamp, avroEvent);
    }
}