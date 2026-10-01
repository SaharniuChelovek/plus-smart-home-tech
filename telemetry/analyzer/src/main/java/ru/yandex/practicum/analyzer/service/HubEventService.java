package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.model.Action;
import ru.yandex.practicum.analyzer.model.ActionType;
import ru.yandex.practicum.analyzer.model.Condition;
import ru.yandex.practicum.analyzer.model.ConditionOperation;
import ru.yandex.practicum.analyzer.model.ConditionType;
import ru.yandex.practicum.analyzer.model.Scenario;
import ru.yandex.practicum.analyzer.model.ScenarioAction;
import ru.yandex.practicum.analyzer.model.ScenarioActionId;
import ru.yandex.practicum.analyzer.model.ScenarioCondition;
import ru.yandex.practicum.analyzer.model.ScenarioConditionId;
import ru.yandex.practicum.analyzer.model.Sensor;
import ru.yandex.practicum.analyzer.repository.ActionRepository;
import ru.yandex.practicum.analyzer.repository.ConditionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioActionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioConditionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.analyzer.repository.SensorRepository;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubEventService {

    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;

    @Transactional
    public void handle(HubEventAvro event) {
        String hubId = event.getHubId();
        Object payload = event.getPayload();

        switch (payload) {
            case DeviceAddedEventAvro e -> addDevice(hubId, e);
            case DeviceRemovedEventAvro e -> removeDevice(hubId, e);
            case ScenarioAddedEventAvro e -> addScenario(hubId, e);
            case ScenarioRemovedEventAvro e -> removeScenario(hubId, e);
            default -> log.warn("Неизвестный тип события хаба {}: {}", hubId, payload);
        }
    }

    private void addDevice(String hubId, DeviceAddedEventAvro event) {
        if (sensorRepository.existsById(event.getId())) {
            log.debug("Устройство {} уже зарегистрировано, событие игнорируется", event.getId());
            return;
        }
        sensorRepository.save(new Sensor(event.getId(), hubId));
    }

    private void removeDevice(String hubId, DeviceRemovedEventAvro event) {
        Optional<Sensor> sensor = sensorRepository.findByIdAndHubId(event.getId(), hubId);
        if (sensor.isEmpty()) {
            return;
        }

        List<ScenarioCondition> conditionLinks = scenarioConditionRepository.findBySensor_Id(event.getId());
        List<ScenarioAction> actionLinks = scenarioActionRepository.findBySensor_Id(event.getId());

        List<Long> conditionIds = conditionLinks.stream().map(l -> l.getCondition().getId()).toList();
        List<Long> actionIds = actionLinks.stream().map(l -> l.getAction().getId()).toList();

        scenarioConditionRepository.deleteAll(conditionLinks);
        scenarioActionRepository.deleteAll(actionLinks);
        scenarioConditionRepository.flush();

        conditionRepository.deleteAllById(conditionIds);
        actionRepository.deleteAllById(actionIds);
        sensorRepository.delete(sensor.get());
    }

    private void addScenario(String hubId, ScenarioAddedEventAvro event) {
        // повторное добавление сценария с тем же названием — это обновление
        scenarioRepository.findByHubIdAndName(hubId, event.getName()).ifPresent(this::deleteScenario);

        Set<String> sensorIds = new HashSet<>();
        event.getConditions().forEach(c -> sensorIds.add(c.getSensorId()));
        event.getActions().forEach(a -> sensorIds.add(a.getSensorId()));

        Map<String, Sensor> sensors = sensorRepository.findAllById(sensorIds).stream()
                .filter(s -> hubId.equals(s.getHubId()))
                .collect(Collectors.toMap(Sensor::getId, Function.identity()));

        if (!sensors.keySet().containsAll(sensorIds)) {
            log.warn("Сценарий '{}' хаба {} ссылается на незарегистрированные устройства, событие пропущено",
                    event.getName(), hubId);
            return;
        }

        Scenario scenario = new Scenario();
        scenario.setHubId(hubId);
        scenario.setName(event.getName());
        scenario = scenarioRepository.save(scenario);

        for (ScenarioConditionAvro c : event.getConditions()) {
            Condition condition = conditionRepository.save(toCondition(c));
            Sensor sensor = sensors.get(c.getSensorId());
            ScenarioConditionId id = new ScenarioConditionId(scenario.getId(), sensor.getId(), condition.getId());
            scenarioConditionRepository.save(new ScenarioCondition(id, scenario, sensor, condition));
        }

        for (DeviceActionAvro a : event.getActions()) {
            Action action = actionRepository.save(toAction(a));
            Sensor sensor = sensors.get(a.getSensorId());
            ScenarioActionId id = new ScenarioActionId(scenario.getId(), sensor.getId(), action.getId());
            scenarioActionRepository.save(new ScenarioAction(id, scenario, sensor, action));
        }
    }

    private void removeScenario(String hubId, ScenarioRemovedEventAvro event) {
        scenarioRepository.findByHubIdAndName(hubId, event.getName()).ifPresent(this::deleteScenario);
    }

    private void deleteScenario(Scenario scenario) {
        List<Long> conditionIds = scenario.getScenarioConditions().stream()
                .map(l -> l.getCondition().getId()).toList();
        List<Long> actionIds = scenario.getScenarioActions().stream()
                .map(l -> l.getAction().getId()).toList();

        // каскад удалит строки связей, затем можно удалять сами условия и действия
        scenarioRepository.delete(scenario);
        scenarioRepository.flush();

        conditionRepository.deleteAllById(conditionIds);
        actionRepository.deleteAllById(actionIds);
    }

    private Condition toCondition(ScenarioConditionAvro c) {
        Condition condition = new Condition();
        condition.setType(ConditionType.valueOf(c.getType().name()));
        condition.setOperation(ConditionOperation.valueOf(c.getOperation().name()));
        condition.setValue(toInteger(c.getValue()));
        return condition;
    }

    private Action toAction(DeviceActionAvro a) {
        Action action = new Action();
        action.setType(ActionType.valueOf(a.getType().name()));
        action.setValue(a.getValue());
        return action;
    }

    // в Avro значение условия — union {null, int, boolean}, в БД хранится INTEGER
    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean b) {
            return b ? 1 : 0;
        }
        return ((Number) value).intValue();
    }
}