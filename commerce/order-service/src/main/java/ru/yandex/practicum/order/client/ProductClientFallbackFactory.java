package ru.yandex.practicum.order.client;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;

import java.util.Optional;

@Slf4j
@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    @Override
    public ProductClient create(Throwable cause) {
        return new ProductClient() {
            @Override
            public ProductResponse getById(Long id) {
                Optional<FeignException> business = ClientFailures.businessError(cause);
                if (business.isPresent()) {
                    throw business.get();
                }
                log.warn("product-service недоступен: операция=getById, productId={}, причина={}",
                        id, ClientFailures.describe(cause));
                throw new ProductServiceUnavailableException(id, cause);
            }
        };
    }
}
