package ru.yandex.practicum.order;


import ru.yandex.practicum.order.client.InventoryClient;
import ru.yandex.practicum.order.client.ProductClient;
import ru.yandex.practicum.order.client.ProductResponse;
import ru.yandex.practicum.order.client.ReserveRequest;
import ru.yandex.practicum.order.client.ReserveResponse;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.entity.OrderStatus;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.mapper.OrderMapper;
import ru.yandex.practicum.order.repository.OrderRepository;
import ru.yandex.practicum.order.service.OrderPersistenceService;
import ru.yandex.practicum.order.service.OrderService;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderFeignTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductClient productClient;
    @Mock
    private InventoryClient inventoryClient;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        OrderMapper orderMapper = new OrderMapper();
        OrderPersistenceService persistenceService =
                new OrderPersistenceService(orderRepository, orderMapper);
        orderService = new OrderService(
                orderRepository, orderMapper, productClient, inventoryClient, persistenceService);
    }

    @Test
    void createOrder_success_reservesStockAndSavesConfirmedOrder() {
        when(productClient.getById(1L)).thenReturn(product(1L, "Ноутбук", "100.00", true));
        when(inventoryClient.reserve(new ReserveRequest(1L, 2))).thenReturn(okResponse());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDto result = orderService.createOrder(request(item(1L, 2)));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("200.00");
        assertThat(saved.getItems()).hasSize(1);
        OrderItem savedItem = saved.getItems().get(0);
        assertThat(savedItem.getProductId()).isEqualTo(1L);
        assertThat(savedItem.getProductName()).isEqualTo("Ноутбук");
        assertThat(savedItem.getPrice()).isEqualByComparingTo("100.00");
        assertThat(savedItem.getQuantity()).isEqualTo(2);

        assertThat(result.status()).isEqualTo("CONFIRMED");
        verify(inventoryClient, never()).release(any());
    }

    @Test
    void createOrder_duplicateProductId_loadsProductOnceAndReservesTotalQuantity() {
        when(productClient.getById(1L)).thenReturn(product(1L, "Ноутбук", "100.00", true));
        when(inventoryClient.reserve(new ReserveRequest(1L, 3))).thenReturn(okResponse());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.createOrder(request(item(1L, 2), item(1L, 1)));

        verify(productClient, times(1)).getById(1L);
        verify(inventoryClient, times(1)).reserve(any());
        verify(inventoryClient).reserve(new ReserveRequest(1L, 3));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getItems()).hasSize(2);
        assertThat(captor.getValue().getTotalPrice()).isEqualByComparingTo("300.00");
    }

    @Test
    void createOrder_inactiveProduct_doesNotReserveAndDoesNotSave() {
        when(productClient.getById(1L)).thenReturn(product(1L, "Ноутбук", "100.00", false));

        assertThatThrownBy(() -> orderService.createOrder(request(item(1L, 1))))
                .isInstanceOf(OrderProcessingException.class)
                .hasMessageContaining("снят с продажи");

        verifyNoInteractions(inventoryClient);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_productNotFound_doesNotReserveAndDoesNotSave() {
        when(productClient.getById(1L)).thenThrow(feignError(404));

        assertThatThrownBy(() -> orderService.createOrder(request(item(1L, 1))))
                .isInstanceOf(OrderProcessingException.class)
                .hasMessageContaining("не найден");

        verifyNoInteractions(inventoryClient);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_insufficientStock_doesNotSave() {
        when(productClient.getById(1L)).thenReturn(product(1L, "Ноутбук", "100.00", true));
        when(inventoryClient.reserve(any())).thenThrow(feignError(409));

        assertThatThrownBy(() -> orderService.createOrder(request(item(1L, 5))))
                .isInstanceOf(OrderProcessingException.class)
                .hasMessageContaining("Недостаточно товара");

        verify(orderRepository, never()).save(any());
        verify(inventoryClient, never()).release(any()); // ничего не было зарезервировано
    }

    @Test
    void createOrder_inventoryRecordNotFound_doesNotSave() {
        when(productClient.getById(1L)).thenReturn(product(1L, "Ноутбук", "100.00", true));
        when(inventoryClient.reserve(any())).thenThrow(feignError(404));

        assertThatThrownBy(() -> orderService.createOrder(request(item(1L, 1))))
                .isInstanceOf(OrderProcessingException.class)
                .hasMessageContaining("не найдена");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_inventoryUnexpectedError_returnsOrderLevelMessage() {
        when(productClient.getById(1L)).thenReturn(product(1L, "Ноутбук", "100.00", true));
        when(inventoryClient.reserve(any())).thenThrow(feignError(500));

        assertThatThrownBy(() -> orderService.createOrder(request(item(1L, 1))))
                .isInstanceOf(OrderProcessingException.class)
                .hasMessageContaining("Не удалось зарезервировать")
                .hasMessageNotContaining("500"); // детали соседнего сервиса не утекают

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_secondReserveFails_releasesFirstReserve() {
        when(productClient.getById(1L)).thenReturn(product(1L, "Ноутбук", "100.00", true));
        when(productClient.getById(2L)).thenReturn(product(2L, "Мышь", "10.00", true));
        when(inventoryClient.reserve(new ReserveRequest(1L, 1))).thenReturn(okResponse());
        when(inventoryClient.reserve(new ReserveRequest(2L, 1))).thenThrow(feignError(409));

        assertThatThrownBy(() -> orderService.createOrder(request(item(1L, 1), item(2L, 1))))
                .isInstanceOf(OrderProcessingException.class);

        verify(inventoryClient).release(new ReserveRequest(1L, 1));
        verify(inventoryClient, never()).release(new ReserveRequest(2L, 1));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_saveFailsAfterReserve_releasesAllReservesInReverseOrder() {
        when(productClient.getById(1L)).thenReturn(product(1L, "Ноутбук", "100.00", true));
        when(productClient.getById(2L)).thenReturn(product(2L, "Мышь", "10.00", true));
        when(inventoryClient.reserve(any())).thenReturn(okResponse());
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> orderService.createOrder(request(item(1L, 2), item(2L, 3))))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db down");

        InOrder inOrder = inOrder(inventoryClient);
        inOrder.verify(inventoryClient).release(new ReserveRequest(2L, 3));
        inOrder.verify(inventoryClient).release(new ReserveRequest(1L, 2));
    }

    @Test
    void createOrder_releaseFails_originalErrorIsPreservedAndOtherReservesStillReleased() {
        when(productClient.getById(1L)).thenReturn(product(1L, "Ноутбук", "100.00", true));
        when(productClient.getById(2L)).thenReturn(product(2L, "Мышь", "10.00", true));
        when(inventoryClient.reserve(any())).thenReturn(okResponse());
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("db down"));
        when(inventoryClient.release(new ReserveRequest(2L, 1))).thenThrow(feignError(500));

        assertThatThrownBy(() -> orderService.createOrder(request(item(1L, 1), item(2L, 1))))
                .hasMessage("db down");

        verify(inventoryClient).release(new ReserveRequest(1L, 1));
    }

    private static CreateOrderRequest request(OrderItemRequest... items) {
        return new CreateOrderRequest("Иван Петров", "ivan@example.com", List.of(items));
    }

    private static OrderItemRequest item(Long productId, int quantity) {
        return new OrderItemRequest(productId, quantity);
    }

    private static ProductResponse product(Long id, String name, String price, boolean active) {
        return new ProductResponse(id, name, new BigDecimal(price), active);
    }

    private static ReserveResponse okResponse() {
        return new ReserveResponse(true, 10, "ok");
    }

    private static FeignException feignError(int status) {
        Request req = Request.create(Request.HttpMethod.POST, "/test",
                Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
        Response response = Response.builder()
                .status(status)
                .reason("error")
                .request(req)
                .headers(Map.of())
                .build();
        return FeignException.errorStatus("test", response);
    }

    @Test
    void createOrder_productServiceUnavailable_savesPendingOrderWithPlaceholder() {
        when(productClient.getById(1L))
                .thenThrow(new ProductServiceUnavailableException(1L, new RuntimeException("timeout")));
        when(inventoryClient.reserve(new ReserveRequest(1L, 2))).thenReturn(okResponse());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDto result = orderService.createOrder(request(item(1L, 2)));

        Order saved = savedOrder();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
        OrderItem savedItem = saved.getItems().get(0);
        assertThat(savedItem.getProductId()).isEqualTo(1L);
        assertThat(savedItem.getProductName()).isEqualTo("Товар #1 (ожидает проверки)");
        assertThat(savedItem.getPrice()).isEqualByComparingTo("0");
        assertThat(result.status()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(result.statusDetails()).contains("ручной проверки");
        verify(inventoryClient).reserve(new ReserveRequest(1L, 2));
    }

    @Test
    void createOrder_inventoryServiceUnavailable_savesPendingOrderWithProductSnapshot() {
        when(productClient.getById(1L)).thenReturn(product(1L, "Ноутбук", "100.00", true));
        when(inventoryClient.reserve(any()))
                .thenThrow(new InventoryServiceUnavailableException(1L, new RuntimeException("timeout")));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.createOrder(request(item(1L, 2)));

        Order saved = savedOrder();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
        assertThat(saved.getItems().get(0).getProductName()).isEqualTo("Ноутбук");
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("200.00");
        verify(inventoryClient, never()).release(any()); // ничего не было зарезервировано
    }

    @Test
    void createOrder_inventoryUnavailableOnSecondItem_releasesFirstReserveAndSavesPending() {
        when(productClient.getById(1L)).thenReturn(product(1L, "Ноутбук", "100.00", true));
        when(productClient.getById(2L)).thenReturn(product(2L, "Мышь", "10.00", true));
        when(inventoryClient.reserve(new ReserveRequest(1L, 1))).thenReturn(okResponse());
        when(inventoryClient.reserve(new ReserveRequest(2L, 1)))
                .thenThrow(new InventoryServiceUnavailableException(2L, new RuntimeException("down")));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.createOrder(request(item(1L, 1), item(2L, 1)));

        verify(inventoryClient).release(new ReserveRequest(1L, 1));
        assertThat(savedOrder().getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
    }

    private Order savedOrder() {
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        return captor.getValue();
    }
}
