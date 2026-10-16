package ru.yandex.practicum.inventory.exception;

/*
 * Конфликт состояния складской записи (дубликат productId,
 * попытка уменьшить quantity ниже reservedQuantity). Возвращается как 409.
 */
public class InventoryConflictException extends RuntimeException {

    public InventoryConflictException(String message) {
        super(message);
    }
}
