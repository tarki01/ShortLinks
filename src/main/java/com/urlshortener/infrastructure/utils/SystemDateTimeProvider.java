package com.urlshortener.infrastructure.utils;

import com.urlshortener.core.ports.output.DateTimeProvider;

import java.time.LocalDateTime;

/**
 * Реализация провайдера времени (системное время)
 */
public class SystemDateTimeProvider implements DateTimeProvider {

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }

    @Override
    public LocalDateTime plusHours(long hours) {
        return LocalDateTime.now().plusHours(hours);
    }

    @Override
    public LocalDateTime plusDays(long days) {
        return LocalDateTime.now().plusDays(days);
    }
}