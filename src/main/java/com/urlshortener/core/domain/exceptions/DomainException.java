package com.urlshortener.core.domain.exceptions;

/**
 * Базовое исключение доменного слоя
 */
public abstract class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}