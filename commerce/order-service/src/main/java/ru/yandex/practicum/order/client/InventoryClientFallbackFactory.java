package ru.yandex.practicum.order.client;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;

import java.util.Optional;

@Slf4j
@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {

    @Override
    public InventoryClient create(Throwable cause) {
        return new InventoryClient() {
            @Override
            public ReserveResponse reserve(ReserveRequest request) {
                return fail("reserve", request, cause);
            }

            @Override
            public ReserveResponse release(ReserveRequest request) {
                return fail("release", request, cause);
            }
        };
    }

    private ReserveResponse fail(String operation, ReserveRequest request, Throwable cause) {
        Optional<FeignException> business = ClientFailures.businessError(cause);
        if (business.isPresent()) {
            throw business.get(); // 404 / 409 / 400: не маскируем
        }
        log.warn("inventory-service недоступен: операция={}, productId={}, quantity={}, причина={}",
                operation, request.productId(), request.quantity(), ClientFailures.describe(cause));
        throw new InventoryServiceUnavailableException(request.productId(), cause);
    }
}
