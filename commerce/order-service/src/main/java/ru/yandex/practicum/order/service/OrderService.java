package ru.yandex.practicum.order.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.client.InventoryClient;
import ru.yandex.practicum.order.client.ProductClient;
import ru.yandex.practicum.order.client.ProductResponse;
import ru.yandex.practicum.order.client.ReserveRequest;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.exception.NotFoundException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.mapper.OrderMapper;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final OrderPersistenceService persistenceService;

    public OrderDto createOrder(CreateOrderRequest request) {
        Map<Long, ProductResponse> products = new LinkedHashMap<>();
        request.items().stream()
                .map(OrderItemRequest::productId)
                .distinct()
                .forEach(id -> products.put(id, loadActiveProduct(id)));

        Map<Long, Integer> totals = request.items().stream()
                .collect(Collectors.groupingBy(
                        OrderItemRequest::productId,
                        LinkedHashMap::new,
                        Collectors.summingInt(OrderItemRequest::quantity)));

        List<ReserveRequest> reserved = new ArrayList<>();
        try {
            for (Map.Entry<Long, Integer> e : totals.entrySet()) {
                ReserveRequest r = new ReserveRequest(e.getKey(), e.getValue());
                reserve(r);
                reserved.add(r);
            }
            return persistenceService.save(request, products);
        } catch (RuntimeException e) {
            compensate(reserved);
            throw e;
        }
    }

    private ProductResponse loadActiveProduct(Long id) {
        ProductResponse product;
        try {
            product = productClient.getById(id);
        } catch (FeignException.NotFound e) {
            throw new OrderProcessingException("Товар с id=" + id + " не найден");
        } catch (FeignException e) {
            log.error("product-service error: productId={}, status={}", id, e.status(), e);
            throw new OrderProcessingException("Не удалось получить данные товара с id=" + id);
        }
        if (!Boolean.TRUE.equals(product.active())) {
            throw new OrderProcessingException("Товар с id=" + id + " снят с продажи");
        }
        if (product.price() == null) {
            throw new OrderProcessingException("Не удалось определить цену товара с id=" + id);
        }
        return product;
    }

    private void reserve(ReserveRequest r) {
        try {
            inventoryClient.reserve(r);
        } catch (FeignException.NotFound e) {
            throw new OrderProcessingException(
                    "Складская запись для товара с id=" + r.productId() + " не найдена");
        } catch (FeignException.Conflict e) {
            throw new OrderProcessingException(
                    "Недостаточно товара с id=" + r.productId() + " на складе");
        } catch (FeignException e) {
            log.error("inventory-service error: productId={}, status={}", r.productId(), e.status(), e);
            throw new OrderProcessingException(
                    "Не удалось зарезервировать товар с id=" + r.productId());
        }
    }

    private void compensate(List<ReserveRequest> reserved) {
        for (int i = reserved.size() - 1; i >= 0; i--) {
            release(reserved.get(i));
        }
    }

    private void release(ReserveRequest r) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                inventoryClient.release(r);
                return;
            } catch (FeignException.Conflict e) {
                log.warn("Конфликт при снятии резерва productId={}, попытка {}", r.productId(), attempt);
            } catch (Exception e) {
                log.error("Не удалось снять резерв productId={}, qty={}", r.productId(), r.quantity(), e);
                return;
            }
        }
        log.error("Резерв не снят после повторной попытки: productId={}, qty={}",
                r.productId(), r.quantity());
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(orderMapper::toItemDto)
                .orElseThrow(() -> new NotFoundException("Заказ с id " + id + " не найден"));
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toItemDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersByEmail(String email) {
        return orderRepository.findByCustomerEmailIgnoreCase(email).stream()
                .map(orderMapper::toItemDto)
                .toList();
    }

    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}