package ru.yandex.practicum.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "inventory-service",
        fallbackFactory = InventoryClientFallbackFactory.class
)
public interface InventoryClient {

    @PostMapping("/api/inventory/reserve")
    ReserveResponse reserve(@RequestBody ReserveRequest request);

    @PostMapping("/api/inventory/release")
    ReserveResponse release(@RequestBody ReserveRequest request);
}
