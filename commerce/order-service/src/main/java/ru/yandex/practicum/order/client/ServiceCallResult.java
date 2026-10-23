package ru.yandex.practicum.order.client;

public final class ServiceCallResult<T> {

    private enum Type { SUCCESS, FAILURE, DEGRADED }

    private final Type type;
    private final T value;
    private final String message;

    private ServiceCallResult(Type type, T value, String message) {
        this.type = type;
        this.value = value;
        this.message = message;
    }

    public static <T> ServiceCallResult<T> success(T value) {
        return new ServiceCallResult<>(Type.SUCCESS, value, null);
    }

    /** Бизнес-отказ соседа: заказ отклоняется. */
    public static <T> ServiceCallResult<T> failure(String message) {
        return new ServiceCallResult<>(Type.FAILURE, null, message);
    }

    /** Техническая недоступность: заказ можно принять на проверку. */
    public static <T> ServiceCallResult<T> degraded(String reason) {
        return new ServiceCallResult<>(Type.DEGRADED, null, reason);
    }

    public boolean isSuccess()  { return type == Type.SUCCESS; }
    public boolean isFailure()  { return type == Type.FAILURE; }
    public boolean isDegraded() { return type == Type.DEGRADED; }
    public T value()            { return value; }
    public String message()     { return message; }
}
