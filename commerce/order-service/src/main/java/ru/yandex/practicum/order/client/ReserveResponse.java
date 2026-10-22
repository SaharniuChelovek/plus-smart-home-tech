package ru.yandex.practicum.order.client;

public record ReserveResponse(
        boolean success,
        Integer availableQuantity,
        String message
) {}
