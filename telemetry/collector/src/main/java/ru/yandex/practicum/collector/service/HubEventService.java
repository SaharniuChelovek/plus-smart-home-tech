package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.mapper.HubEventMapper;
import ru.yandex.practicum.collector.model.hub.HubEvent;
import ru.yandex.practicum.collector.producer.KafkaEventProducer;

@Service
@RequiredArgsConstructor
public class HubEventService {

    private final HubEventMapper mapper;
    private final KafkaEventProducer producer;

    public void collect(HubEvent event) {
        var avroEvent = mapper.mapToAvro(event);
        producer.sendHubEvent(event.getHubId(), avroEvent);
    }
}