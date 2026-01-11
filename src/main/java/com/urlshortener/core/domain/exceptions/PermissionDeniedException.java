package com.urlshortener.core.domain.exceptions;

/**
 * Исключение: нет прав доступа
 */
public class PermissionDeniedException extends DomainException {
    public PermissionDeniedException(String message) {
        super(message);
    }
}