package ru.yandex.practicum.analyzer.client;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;

@Slf4j
@Component
public class HubRouterClient {

    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub client;

    public HubRouterClient(@GrpcClient("hub-router")
                           HubRouterControllerGrpc.HubRouterControllerBlockingStub client) {
        this.client = client;
    }

    public void send(DeviceActionRequest request) {
        try {
            client.handleDeviceAction(request);
            log.info("Команда отправлена: хаб={}, сценарий={}, устройство={}",
                    request.getHubId(), request.getScenarioName(), request.getAction().getSensorId());
        } catch (StatusRuntimeException e) {
            log.error("Не удалось отправить команду хабу {} (сценарий {}): {}",
                    request.getHubId(), request.getScenarioName(), e.getStatus(), e);
        }
    }
}