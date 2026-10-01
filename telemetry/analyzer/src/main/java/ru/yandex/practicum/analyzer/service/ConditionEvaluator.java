package ru.yandex.practicum.analyzer.service;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.model.Condition;
import ru.yandex.practicum.analyzer.model.ConditionOperation;
import ru.yandex.practicum.analyzer.model.ConditionType;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Component
public class ConditionEvaluator {

    // как достать числовое значение показания из данных датчика
    private static final Map<ConditionType, Function<Object, Optional<Integer>>> EXTRACTORS = Map.of(
            ConditionType.MOTION, ConditionEvaluator::motion,
            ConditionType.LUMINOSITY, ConditionEvaluator::luminosity,
            ConditionType.SWITCH, ConditionEvaluator::switchState,
            ConditionType.TEMPERATURE, ConditionEvaluator::temperature,
            ConditionType.CO2LEVEL, ConditionEvaluator::co2Level,
            ConditionType.HUMIDITY, ConditionEvaluator::humidity
    );

    public boolean isSatisfied(Condition condition, SensorStateAvro state) {
        if (state == null || condition.getValue() == null) {
            return false;
        }
        return EXTRACTORS.get(condition.getType())
                .apply(state.getData())
                .map(actual -> compare(condition.getOperation(), actual, condition.getValue()))
                .orElse(false);
    }

    private static boolean compare(ConditionOperation operation, int actual, int expected) {
        return switch (operation) {
            case EQUALS -> actual == expected;
            case GREATER_THAN -> actual > expected;
            case LOWER_THAN -> actual < expected;
        };
    }

    private static Optional<Integer> motion(Object data) {
        return data instanceof MotionSensorAvro s ? Optional.of(s.getMotion() ? 1 : 0) : Optional.empty();
    }

    private static Optional<Integer> luminosity(Object data) {
        return data instanceof LightSensorAvro s ? Optional.of(s.getLuminosity()) : Optional.empty();
    }

    private static Optional<Integer> switchState(Object data) {
        return data instanceof SwitchSensorAvro s ? Optional.of(s.getState() ? 1 : 0) : Optional.empty();
    }

    private static Optional<Integer> temperature(Object data) {
        return switch (data) {
            case ClimateSensorAvro s -> Optional.of(s.getTemperatureC());
            case TemperatureSensorAvro s -> Optional.of(s.getTemperatureC());
            default -> Optional.empty();
        };
    }

    private static Optional<Integer> co2Level(Object data) {
        return data instanceof ClimateSensorAvro s ? Optional.of(s.getCo2Level()) : Optional.empty();
    }

    private static Optional<Integer> humidity(Object data) {
        return data instanceof ClimateSensorAvro s ? Optional.of(s.getHumidity()) : Optional.empty();
    }
}