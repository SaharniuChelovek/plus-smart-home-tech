package ru.yandex.practicum.collector.mapper;

import com.google.protobuf.Timestamp;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.util.List;

@Component
public class HubEventMapper {

    public HubEventAvro mapToAvro(HubEventProto proto) {
        Object payload = switch (proto.getPayloadCase()) {
            case DEVICE_ADDED -> mapDeviceAdded(proto.getDeviceAdded());
            case DEVICE_REMOVED -> mapDeviceRemoved(proto.getDeviceRemoved());
            case SCENARIO_ADDED -> mapScenarioAdded(proto.getScenarioAdded());
            case SCENARIO_REMOVED -> mapScenarioRemoved(proto.getScenarioRemoved());
            case PAYLOAD_NOT_SET -> throw new IllegalArgumentException(
                    "Не задан payload события хаба: " + proto.getHubId());
        };

        return HubEventAvro.newBuilder()
                .setHubId(proto.getHubId())
                .setTimestamp(toInstant(proto.getTimestamp()))
                .setPayload(payload)
                .build();
    }

    private DeviceAddedEventAvro mapDeviceAdded(DeviceAddedEventProto p) {
        return DeviceAddedEventAvro.newBuilder()
                .setId(p.getId())
                .setType(DeviceTypeAvro.valueOf(p.getType().name()))
                .build();
    }

    private DeviceRemovedEventAvro mapDeviceRemoved(DeviceRemovedEventProto p) {
        return DeviceRemovedEventAvro.newBuilder()
                .setId(p.getId())
                .build();
    }

    private ScenarioAddedEventAvro mapScenarioAdded(ScenarioAddedEventProto p) {
        return ScenarioAddedEventAvro.newBuilder()
                .setName(p.getName())
                .setConditions(mapConditions(p.getConditionList()))
                .setActions(mapActions(p.getActionList()))
                .build();
    }

    private ScenarioRemovedEventAvro mapScenarioRemoved(ScenarioRemovedEventProto p) {
        return ScenarioRemovedEventAvro.newBuilder()
                .setName(p.getName())
                .build();
    }

    private List<ScenarioConditionAvro> mapConditions(List<ScenarioConditionProto> conditions) {
        return conditions.stream()
                .map(c -> ScenarioConditionAvro.newBuilder()
                        .setSensorId(c.getSensorId())
                        .setType(ConditionTypeAvro.valueOf(c.getType().name()))
                        .setOperation(ConditionOperationAvro.valueOf(c.getOperation().name()))
                        .setValue(mapConditionValue(c))
                        .build())
                .toList();
    }

    private Object mapConditionValue(ScenarioConditionProto c) {
        return switch (c.getValueCase()) {
            case BOOL_VALUE -> c.getBoolValue();
            case INT_VALUE -> c.getIntValue();
            case VALUE_NOT_SET -> null;
        };
    }

    private List<DeviceActionAvro> mapActions(List<DeviceActionProto> actions) {
        return actions.stream()
                .map(a -> DeviceActionAvro.newBuilder()
                        .setSensorId(a.getSensorId())
                        .setType(ActionTypeAvro.valueOf(a.getType().name()))
                        .setValue(a.hasValue() ? a.getValue() : null)
                        .build())
                .toList();
    }

    private Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}