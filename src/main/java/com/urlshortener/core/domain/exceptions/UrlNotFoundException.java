package com.urlshortener.core.domain.exceptions;

/**
 * Исключение: ссылка не найдена
 */
public class UrlNotFoundException extends DomainException {
    public UrlNotFoundException(String shortCode) {
        super("URL not found with short code: " + shortCode);
    }
}