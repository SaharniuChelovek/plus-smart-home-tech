package ru.yandex.practicum.order.exception;

public class ProductServiceUnavailableException extends RuntimeException {
    public ProductServiceUnavailableException(Long productId, Throwable cause) {
        super("product-service недоступен: productId=" + productId, cause);
    }
}