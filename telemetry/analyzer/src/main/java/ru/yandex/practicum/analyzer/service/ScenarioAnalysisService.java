package ru.yandex.practicum.analyzer.service;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.model.Action;
import ru.yandex.practicum.analyzer.model.Scenario;
import ru.yandex.practicum.analyzer.model.ScenarioAction;
import ru.yandex.practicum.analyzer.model.ScenarioCondition;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScenarioAnalysisService {

    private final ScenarioRepository scenarioRepository;
    private final ConditionEvaluator conditionEvaluator;

    /**
     * Возвращает команды для всех сценариев хаба, условия которых выполнены в снапшоте.
     * Транзакция нужна для ленивой загрузки условий и действий; сами команды
     * не содержат ссылок на сущности, поэтому отправлять их можно уже после её завершения.
     */
    @Transactional(readOnly = true)
    public List<DeviceActionRequest> analyze(SensorsSnapshotAvro snapshot) {
        Map<String, SensorStateAvro> states = snapshot.getSensorsState();

        return scenarioRepository.findByHubId(snapshot.getHubId()).stream()
                .filter(scenario -> isTriggered(scenario, states))
                .flatMap(scenario -> toRequests(scenario).stream())
                .toList();
    }

    private boolean isTriggered(Scenario scenario, Map<String, SensorStateAvro> states) {
        List<ScenarioCondition> conditions = scenario.getScenarioConditions();
        return !conditions.isEmpty() && conditions.stream()
                .allMatch(link -> conditionEvaluator.isSatisfied(
                        link.getCondition(), states.get(link.getSensor().getId())));
    }

    private List<DeviceActionRequest> toRequests(Scenario scenario) {
        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();

        return scenario.getScenarioActions().stream()
                .map(link -> DeviceActionRequest.newBuilder()
                        .setHubId(scenario.getHubId())
                        .setScenarioName(scenario.getName())
                        .setAction(toProto(link))
                        .setTimestamp(timestamp)
                        .build())
                .toList();
    }

    private DeviceActionProto toProto(ScenarioAction link) {
        Action action = link.getAction();
        DeviceActionProto.Builder builder = DeviceActionProto.newBuilder()
                .setSensorId(link.getSensor().getId())
                .setType(ActionTypeProto.valueOf(action.getType().name()));
        if (action.getValue() != null) {
            builder.setValue(action.getValue());
        }
        return builder.build();
    }
}