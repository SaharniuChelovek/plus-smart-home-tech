package ru.yandex.practicum.collector.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.collector.model.hub.HubEvent;
import ru.yandex.practicum.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.collector.service.HubEventService;
import ru.yandex.practicum.collector.service.SensorEventService;

@RestController
@RequiredArgsConstructor
public class EventController {

    private final SensorEventService sensorEventService;
    private final HubEventService hubEventService;

    @PostMapping("/events/sensors")
    public void collectSensorEvent(@Valid @RequestBody SensorEvent event) {
        sensorEventService.collect(event);
    }

    @PostMapping("/events/hubs")
    public void collectHubEvent(@Valid @RequestBody HubEvent event) {
        hubEventService.collect(event);
    }
}