package ru.yandex.practicum.order.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.client.ProductResponse;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class OrderMapper {

    public Order toEntity(CreateOrderRequest request, Map<Long, ProductResponse> products, OrderStatus status) {
        Order order = Order.builder()
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();

        request.items().forEach(item ->
                order.addItem(toEntity(item, products.get(item.productId()))));
        return order;
    }

    public OrderItem toEntity(OrderItemRequest request, ProductResponse product) {
        if (product == null) {
            // каталог был недоступен: сохраняем то, что известно
            return OrderItem.builder()
                    .productId(request.productId())
                    .productName("Товар #" + request.productId() + " (ожидает проверки)")
                    .quantity(request.quantity())
                    .price(BigDecimal.ZERO)
                    .build();
        }
        return OrderItem.builder()
                .productId(product.id())
                .productName(product.name())
                .quantity(request.quantity())
                .price(product.price())
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
                statusDetails(order.getStatus()),
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

    private String statusDetails(OrderStatus status) {
        return status == OrderStatus.PENDING_CONFIRMATION
                ? "Заказ принят, но требует ручной проверки: данные о товарах или резервирование на складе не подтверждены"
                : null;
    }
}
