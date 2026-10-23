package ru.yandex.practicum.order.client;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

import java.util.Optional;
import java.util.concurrent.TimeoutException;

final class ClientFailures {

    private ClientFailures() {}

    /** Ответ соседнего сервиса 4xx: бизнес-отказ, а не техническая проблема. */
    static Optional<FeignException> businessError(Throwable cause) {
        for (Throwable t = cause; t != null; t = t.getCause()) {
            if (t instanceof FeignException fe && fe.status() >= 400 && fe.status() < 500) {
                return Optional.of(fe);
            }
        }
        return Optional.empty();
    }

    /** Короткое описание причины для логов. */
    static String describe(Throwable cause) {
        if (cause instanceof CallNotPermittedException) {
            return "Circuit Breaker открыт, вызов не отправлялся";
        }
        if (cause instanceof TimeoutException) {
            return "таймаут ожидания ответа";
        }
        if (cause instanceof FeignException fe && fe.status() > 0) {
            return "соседний сервис ответил HTTP " + fe.status();
        }
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}