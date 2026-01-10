package com.urlshortener.core.ports.output;

import java.time.LocalDateTime;

/**
 * Выходной порт для работы с временем (для тестирования)
 */
public interface DateTimeProvider {

    /**
     * Текущее время
     */
    LocalDateTime now();

    /**
     * Текущее время плюс часы
     */
    LocalDateTime plusHours(long hours);

    /**
     * Текущее время плюс дни
     */
    LocalDateTime plusDays(long days);
}