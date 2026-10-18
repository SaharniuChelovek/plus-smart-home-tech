package ru.yandex.practicum.inventory.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.inventory.dto.CreateInventoryRequest;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.InventoryItem;
import ru.yandex.practicum.inventory.exception.InsufficientStockException;
import ru.yandex.practicum.inventory.exception.InventoryConflictException;
import ru.yandex.practicum.inventory.exception.NotFoundException;
import ru.yandex.practicum.inventory.mapper.InventoryMapper;
import ru.yandex.practicum.inventory.repository.InventoryRepository;

import java.util.List;

@RequiredArgsConstructor
@Service
public class InventoryService {

    private final InventoryRepository repository;
    private final InventoryMapper mapper;

    @Transactional(readOnly = true)
    public List<InventoryDto> findAll() {
        return mapper.toDtoList(repository.findAll());
    }

    @Transactional(readOnly = true)
    public InventoryDto findByProductId(Long productId) {
        return mapper.toDto(getOrThrow(productId));
    }

    @Transactional
    public InventoryDto create(CreateInventoryRequest request) {
        if (repository.existsByProductId(request.productId())) {
            throw new InventoryConflictException(
                    "Складская запись для товара с id=" + request.productId() + " уже существует");
        }
        return mapper.toDto(repository.save(mapper.toEntity(request)));
    }

    @Transactional
    public InventoryDto updateQuantity(UpdateInventoryRequest request) {
        InventoryItem item = getOrThrow(request.productId());
        if (request.quantity() < item.getReservedQuantity()) {
            throw new InventoryConflictException(
                    "Нельзя установить количество " + request.quantity() + " для товара с id="
                            + request.productId() + ": уже зарезервировано " + item.getReservedQuantity());
        }
        item.setQuantity(request.quantity());
        return mapper.toDto(repository.save(item));
    }

    @Transactional
    public ReserveResponse reserve(ReserveRequest request) {
        InventoryItem item = getOrThrow(request.productId());
        int available = item.getAvailableQuantity();
        if (request.quantity() > available) {
            throw new InsufficientStockException(
                    "Недостаточно товара с id=" + request.productId()
                            + ": запрошено " + request.quantity() + ", доступно " + available);
        }
        item.setReservedQuantity(item.getReservedQuantity() + request.quantity());
        InventoryItem saved = repository.save(item);
        return mapper.toReserveResponse(saved, "Товар успешно зарезервирован");
    }

    private InventoryItem getOrThrow(Long productId) {
        return repository.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException(
                        "Складская запись для товара с id=" + productId + " не найдена"));
    }
}
