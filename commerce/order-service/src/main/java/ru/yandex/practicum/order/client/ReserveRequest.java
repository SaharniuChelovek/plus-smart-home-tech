package ru.yandex.practicum.order.client;

public record ReserveRequest(
        Long productId,
        Integer quantity
) {}

