package ru.yandex.practicum.order;


import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.order.client.InventoryClientFallbackFactory;
import ru.yandex.practicum.order.client.ProductClientFallbackFactory;
import ru.yandex.practicum.order.client.ReserveRequest;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class ClientFallbackFactoriesTest {

    private final ProductClientFallbackFactory productFactory = new ProductClientFallbackFactory();
    private final InventoryClientFallbackFactory inventoryFactory = new InventoryClientFallbackFactory();

    @Test
    void product_notFound_isRethrownAsIs() {
        FeignException notFound = feignError(404);
        assertThatThrownBy(() -> productFactory.create(notFound).getById(1L)).isSameAs(notFound);
    }

    @Test
    void product_serverError_becomesUnavailable() {
        assertThatThrownBy(() -> productFactory.create(feignError(503)).getById(1L))
                .isInstanceOf(ProductServiceUnavailableException.class);
    }

    @Test
    void inventory_conflict_isRethrownAsIs() {
        FeignException conflict = feignError(409);
        assertThatThrownBy(() -> inventoryFactory.create(conflict).reserve(new ReserveRequest(1L, 1)))
                .isSameAs(conflict);
    }

    @Test
    void inventory_timeout_becomesUnavailable() {
        assertThatThrownBy(() -> inventoryFactory.create(new TimeoutException()).reserve(new ReserveRequest(1L, 1)))
                .isInstanceOf(InventoryServiceUnavailableException.class);
    }

    @Test
    void inventory_release_serverError_becomesUnavailable() {
        assertThatThrownBy(() -> inventoryFactory.create(feignError(500)).release(new ReserveRequest(1L, 1)))
                .isInstanceOf(InventoryServiceUnavailableException.class);
    }

    private static FeignException feignError(int status) {
        Request req = Request.create(Request.HttpMethod.POST, "/test",
                Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
        Response response = Response.builder()
                .status(status).reason("error").request(req).headers(Map.of()).build();
        return FeignException.errorStatus("test", response);
    }
}
