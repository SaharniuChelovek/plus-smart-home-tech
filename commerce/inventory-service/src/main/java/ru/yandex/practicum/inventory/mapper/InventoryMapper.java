package ru.yandex.practicum.inventory.mapper;


import org.springframework.stereotype.Component;
import ru.yandex.practicum.inventory.dto.CreateInventoryRequest;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.entity.InventoryItem;

import java.util.List;

@Component
public class InventoryMapper {

    public InventoryDto toDto(InventoryItem item) {
        return new InventoryDto(
                item.getId(),
                item.getProductId(),
                item.getQuantity(),
                item.getReservedQuantity(),
                item.getAvailableQuantity());
    }

    public List<InventoryDto> toDtoList(List<InventoryItem> items) {
        return items.stream().map(this::toDto).toList();
    }

    public InventoryItem toEntity(CreateInventoryRequest request) {
        return new InventoryItem(request.productId(), request.quantity());
    }

    public ReserveResponse toReserveResponse(InventoryItem item, String message) {
        return new ReserveResponse(true, item.getAvailableQuantity(), message);
    }
}
