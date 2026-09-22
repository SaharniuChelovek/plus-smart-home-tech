package ru.yandex.practicum.collector.mapper;

import com.google.protobuf.Timestamp;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;

@Component
public class SensorEventMapper {

    public SensorEventAvro mapToAvro(SensorEventProto proto) {
        Object payload = switch (proto.getPayloadCase()) {
            case MOTION_SENSOR -> mapMotion(proto.getMotionSensor());
            case TEMPERATURE_SENSOR -> mapTemperature(proto.getTemperatureSensor());
            case LIGHT_SENSOR -> mapLight(proto.getLightSensor());
            case CLIMATE_SENSOR -> mapClimate(proto.getClimateSensor());
            case SWITCH_SENSOR -> mapSwitch(proto.getSwitchSensor());
            case PAYLOAD_NOT_SET -> throw new IllegalArgumentException(
                    "Не задан payload события датчика: " + proto.getId());
        };

        return SensorEventAvro.newBuilder()
                .setId(proto.getId())
                .setHubId(proto.getHubId())
                .setTimestamp(toInstant(proto.getTimestamp()))
                .setPayload(payload)
                .build();
    }

    private ClimateSensorAvro mapClimate(ClimateSensorProto p) {
        return ClimateSensorAvro.newBuilder()
                .setTemperatureC(p.getTemperatureC())
                .setHumidity(p.getHumidity())
                .setCo2Level(p.getCo2Level())
                .build();
    }

    private LightSensorAvro mapLight(LightSensorProto p) {
        return LightSensorAvro.newBuilder()
                .setLinkQuality(p.getLinkQuality())
                .setLuminosity(p.getLuminosity())
                .build();
    }

    private MotionSensorAvro mapMotion(MotionSensorProto p) {
        return MotionSensorAvro.newBuilder()
                .setLinkQuality(p.getLinkQuality())
                .setMotion(p.getMotion())
                .setVoltage(p.getVoltage())
                .build();
    }

    private SwitchSensorAvro mapSwitch(SwitchSensorProto p) {
        return SwitchSensorAvro.newBuilder()
                .setState(p.getState())
                .build();
    }

    private TemperatureSensorAvro mapTemperature(TemperatureSensorProto p) {
        return TemperatureSensorAvro.newBuilder()
                .setTemperatureC(p.getTemperatureC())
                .setTemperatureF(p.getTemperatureF())
                .build();
    }

    private Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}