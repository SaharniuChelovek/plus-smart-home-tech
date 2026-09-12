package ru.yandex.practicum.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.hub.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.List;

@Component
public class HubEventMapper {

    public HubEventAvro mapToAvro(HubEvent event) {
        Object payload = switch (event) {
            case DeviceAddedEvent e -> DeviceAddedEventAvro.newBuilder()
                    .setId(e.getId())
                    .setType(DeviceTypeAvro.valueOf(e.getDeviceType().name()))
                    .build();

            case DeviceRemovedEvent e -> DeviceRemovedEventAvro.newBuilder()
                    .setId(e.getId())
                    .build();

            case ScenarioAddedEvent e -> ScenarioAddedEventAvro.newBuilder()
                    .setName(e.getName())
                    .setConditions(mapConditions(e.getConditions()))
                    .setActions(mapActions(e.getActions()))
                    .build();

            case ScenarioRemovedEvent e -> ScenarioRemovedEventAvro.newBuilder()
                    .setName(e.getName())
                    .build();

            default -> throw new IllegalArgumentException(
                    "Неизвестный тип события хаба: " + event.getType());
        };

        return HubEventAvro.newBuilder()
                .setHub_id(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(payload)
                .build();
    }

    private List<ScenarioConditionAvro> mapConditions(List<ScenarioCondition> conditions) {
        return conditions.stream()
                .map(c -> ScenarioConditionAvro.newBuilder()
                        .setSensor_id(c.getSensorId())
                        .setType(ConditionTypeAvro.valueOf(c.getType().name()))
                        .setOperation(ConditionOperationAvro.valueOf(c.getOperation().name()))
                        .setValue(c.getValue())
                        .build())
                .toList();
    }

    private List<DeviceActionAvro> mapActions(List<DeviceAction> actions) {
        return actions.stream()
                .map(a -> DeviceActionAvro.newBuilder()
                        .setSensor_id(a.getSensorId())
                        .setType(ActionTypeAvro.valueOf(a.getType().name()))
                        .setValue(a.getValue())
                        .build())
                .toList();
    }
}