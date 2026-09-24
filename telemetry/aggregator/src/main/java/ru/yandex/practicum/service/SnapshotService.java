package ru.yandex.practicum.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SnapshotService {

    private final Map<String, SensorsSnapshotAvro> snapshots = new ConcurrentHashMap<>();

    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        SensorsSnapshotAvro snapshot = snapshots.computeIfAbsent(
                event.getHubId(),
                hubId -> SensorsSnapshotAvro.newBuilder()
                        .setHubId(hubId)
                        .setTimestamp(event.getTimestamp())
                        .setSensorsState(new java.util.HashMap<>())
                        .build()
        );

        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();
        SensorStateAvro oldState = sensorsState.get(event.getId());

        if (oldState != null) {
            boolean isOlderOrEqual = !event.getTimestamp().isAfter(oldState.getTimestamp());
            boolean isSameData = oldState.getData().equals(event.getPayload());

            if (isOlderOrEqual || isSameData) {
                return Optional.empty();
            }
        }

        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(event.getPayload())
                .build();

        sensorsState.put(event.getId(), newState);

        SensorsSnapshotAvro updatedSnapshot = SensorsSnapshotAvro.newBuilder()
                .setHubId(snapshot.getHubId())
                .setTimestamp(event.getTimestamp())
                .setSensorsState(sensorsState)
                .build();

        snapshots.put(event.getHubId(), updatedSnapshot);

        return Optional.of(updatedSnapshot);
    }
}
