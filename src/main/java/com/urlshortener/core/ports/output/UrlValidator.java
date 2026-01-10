package com.urlshortener.core.ports.output;

/**
 * Выходной порт для валидации URL
 */
public interface UrlValidator {

    /**
     * Проверить валидность URL
     */
    boolean isValid(String url);

    /**
     * Нормализовать URL
     */
    String normalize(String url);

    /**
     * Проверить, безопасный ли URL (HTTPS)
     */
    boolean isSecure(String url);

    /**
     * Проверить, не является ли URL локальным
     */
    boolean isLocal(String url);
}