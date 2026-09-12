package ru.yandex.practicum.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.sensor.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Component
public class SensorEventMapper {

    public SensorEventAvro mapToAvro(SensorEvent event) {
        Object payload = switch (event) {
            case ClimateSensorEvent e -> ClimateSensorAvro.newBuilder()
                    .setTemperature_c(e.getTemperatureC())
                    .setHumidity(e.getHumidity())
                    .setCo2_level(e.getCo2Level())
                    .build();

            case LightSensorEvent e -> LightSensorAvro.newBuilder()
                    .setLink_quality(e.getLinkQuality())
                    .setLuminosity(e.getLuminosity())
                    .build();

            case MotionSensorEvent e -> MotionSensorAvro.newBuilder()
                    .setLink_quality(e.getLinkQuality())
                    .setMotion(e.isMotion())
                    .setVoltage(e.getVoltage())
                    .build();

            case SwitchSensorEvent e -> SwitchSensorAvro.newBuilder()
                    .setState(e.isState())
                    .build();

            case TemperatureSensorEvent e -> TemperatureSensorAvro.newBuilder()
                    .setTemperature_c(e.getTemperatureC())
                    .setTemperature_f(e.getTemperatureF())
                    .build();

            default -> throw new IllegalArgumentException(
                    "Неизвестный тип события датчика: " + event.getType());
        };

        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHub_id(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(payload)
                .build();
    }
}