package com.urlshortener.core.domain.exceptions;

/**
 * Исключение: ссылка не найдена
 */
public class UrlNotFoundException extends DomainException {
    public UrlNotFoundException(String shortCode) {
        super("URL не найдено с данном коротким кодом: " + shortCode);
    }
}