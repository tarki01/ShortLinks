package com.urlshortener.core.domain.exceptions;

/**
 * Исключение: ошибка валидации
 */
public class ValidationException extends DomainException {
    public ValidationException(String message) {
        super("Validation error: " + message);
    }
}