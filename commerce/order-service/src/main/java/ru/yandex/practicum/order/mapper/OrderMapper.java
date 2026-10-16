package ru.yandex.practicum.order.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderMapper {

    public Order toEntity(CreateOrderRequest request) {
        Order order = Order.builder()
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();

        request.items().forEach(item -> order.addItem(toEntity(item)));

        return order;
    }

    public OrderItem toEntity(OrderItemRequest request) {
        return OrderItem.builder()
                .productId(request.productId())
                .productName(request.productName())
                .quantity(request.quantity())
                .price(request.price())
                .build();
    }

    public OrderDto toItemDto(Order order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(this::toItemDto)
                .toList();

        return new OrderDto(
                order.getId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getStatus().name(),
                order.getTotalPrice(),
                null,
                order.getCreatedAt(),
                items);
    }

    public OrderItemDto toItemDto(OrderItem item) {
        return new OrderItemDto(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPrice());
    }
}
