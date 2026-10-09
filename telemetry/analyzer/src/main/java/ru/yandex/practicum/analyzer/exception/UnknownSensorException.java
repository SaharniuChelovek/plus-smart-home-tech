package ru.yandex.practicum.analyzer.exception;

public class UnknownSensorException extends RuntimeException {
    public UnknownSensorException(String message) {
        super(message);
    }
}
